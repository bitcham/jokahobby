package com.jokahobby.infra.security.oauth2;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.security.jwt.CookieUtil;
import com.jokahobby.infra.security.jwt.JwtProperties;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.modules.account.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OAuth2AuthorizationCodeStore codeStore;
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        Objects.requireNonNull(principal, "OAuth2 principal must not be null");

        Account account = principal.getAccount();

        byte[] bindingBytes = new byte[32];
        SECURE_RANDOM.nextBytes(bindingBytes);
        String binding = HexFormat.of().formatHex(bindingBytes);
        String bindingHash = TokenHashUtil.sha256(binding);

        String deviceInfo = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        boolean nicknameRequired = account.getNickname() == null;

        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                account.getId(), nicknameRequired, deviceInfo, ipAddress,
                bindingHash, Instant.now());

        String code = codeStore.store(data);

        response.addHeader(HttpHeaders.SET_COOKIE,
                CookieUtil.createBindingCookie(binding, jwtProperties.secureCookie()).toString());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(appProperties.getFrontendUrl())
                .path("/oauth2/callback")
                .queryParam("code", code)
                .build().toUriString();

        log.info("OAuth2 login success for accountId={}, code issued", account.getId());
        response.sendRedirect(redirectUrl);
    }
}
