package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.OAuth2CodeRequest;
import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.OAuth2TokenResponse;
import com.jokahobby.api.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jokahobby.infra.exception.AppException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.infra.security.jwt.CookieUtil;
import com.jokahobby.infra.security.jwt.JwtProperties;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.infra.security.oauth2.OAuth2AuthorizationCodeStore;
import com.jokahobby.infra.security.oauth2.OAuth2AuthorizationData;
import com.jokahobby.modules.account.RefreshToken;
import com.jokahobby.modules.account.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class AuthApiController {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AuthorizationCodeStore codeStore;

    @Operation(summary = "Exchange OAuth2 authorization code for tokens")
    @PostMapping("/api/v1/auth/oauth2/token")
    public ResponseEntity<ApiResponse<OAuth2TokenResponse>> exchangeOAuth2Code(
            @Valid @RequestBody OAuth2CodeRequest request,
            @CookieValue(name = "oauth2_binding", required = false) String binding) {

        if (binding == null) {
            throw new AppException(ErrorCode.INVALID_AUTHORIZATION_CODE);
        }

        String bindingHash = TokenHashUtil.sha256(binding);
        OAuth2AuthorizationData data = codeStore.consumeIfValid(request.code(), bindingHash);

        if (data == null) {
            throw new AppException(ErrorCode.INVALID_AUTHORIZATION_CODE);
        }

        String accessToken = jwtProvider.createAccessToken(data.accountId());
        String refreshTokenRaw = jwtProvider.createRefreshToken(data.accountId(), null, 0);
        String refreshTokenHash = TokenHashUtil.sha256(refreshTokenRaw);

        refreshTokenService.createRefreshToken(
                data.accountId(), refreshTokenHash, data.deviceInfo(), data.ipAddress());

        OAuth2TokenResponse tokenResponse = new OAuth2TokenResponse(
                accessToken, jwtProvider.getAccessTokenExpirySeconds(), data.nicknameRequired());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.createRefreshTokenCookie(
                                refreshTokenRaw,
                                jwtProperties.refreshTokenExpiry(),
                                jwtProperties.secureCookie()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.deleteBindingCookie(jwtProperties.secureCookie()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.ok(tokenResponse));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refreshToken") String refreshTokenRaw,
            HttpServletRequest httpRequest) {

        if (!jwtProvider.validateToken(refreshTokenRaw)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String oldHash = TokenHashUtil.sha256(refreshTokenRaw);
        UUID accountId = jwtProvider.getAccountId(refreshTokenRaw);

        String newRefreshTokenRaw = jwtProvider.createRefreshToken(accountId, null, 0);
        String newHash = TokenHashUtil.sha256(newRefreshTokenRaw);

        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(
                oldHash, newHash, deviceInfo, ipAddress);

        String newRefreshCookieValue = newRefreshTokenRaw;
        if (rotated.getTokenHash().equals(oldHash)) {
            newRefreshCookieValue = refreshTokenRaw;
        }

        String accessToken = jwtProvider.createAccessToken(accountId);
        TokenResponse tokenResponse = new TokenResponse(
                accessToken, jwtProvider.getAccessTokenExpirySeconds());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.createRefreshTokenCookie(
                                newRefreshCookieValue,
                                jwtProperties.refreshTokenExpiry(),
                                jwtProperties.secureCookie()).toString())
                .body(ApiResponse.ok(tokenResponse));
    }

    @Operation(summary = "Logout current session")
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenRaw) {

        if (refreshTokenRaw != null) {
            String hash = TokenHashUtil.sha256(refreshTokenRaw);
            refreshTokenService.revokeToken(hash);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.deleteRefreshTokenCookie(
                                jwtProperties.secureCookie()).toString())
                .body(ApiResponse.ok());
    }

    @Operation(summary = "Logout all sessions")
    @PostMapping("/api/v1/auth/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest httpRequest) {

        UUID accountId = getAccountIdFromRequest(httpRequest);
        refreshTokenService.revokeAllTokens(accountId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.deleteRefreshTokenCookie(
                                jwtProperties.secureCookie()).toString())
                .body(ApiResponse.ok());
    }

    private UUID getAccountIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        return jwtProvider.getAccountId(token);
    }
}
