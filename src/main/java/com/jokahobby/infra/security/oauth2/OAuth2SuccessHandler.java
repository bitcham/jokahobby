package com.jokahobby.infra.security.oauth2;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.security.jwt.CookieUtil;
import com.jokahobby.infra.security.jwt.JwtProperties;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        assert principal != null;

        Account account = principal.getAccount();

        String accessToken = jwtProvider.createAccessToken(account.getId());
        String refreshTokenRaw = jwtProvider.createRefreshToken(account.getId(), null, 0);
        String refreshTokenHash = TokenHashUtil.sha256(refreshTokenRaw);

        String deviceInfo = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        refreshTokenService.createRefreshToken(
                account.getId(), refreshTokenHash, deviceInfo, ipAddress);

        response.addHeader(HttpHeaders.SET_COOKIE,
                CookieUtil.createRefreshTokenCookie(
                        refreshTokenRaw,
                        jwtProperties.refreshTokenExpiry(),
                        jwtProperties.secureCookie()).toString());

        long expiresIn = jwtProvider.getAccessTokenExpirySeconds();
        boolean nicknameRequired = account.getNickname() == null;
        String redirectUrl = appProperties.getFrontendUrl()
                + "/oauth2/callback?token=" + accessToken
                + "&expiresIn=" + expiresIn
                + "&nicknameRequired=" + nicknameRequired;

        log.info("OAuth2 login success for accountId={}", account.getId());
        response.sendRedirect(redirectUrl);
    }
}
