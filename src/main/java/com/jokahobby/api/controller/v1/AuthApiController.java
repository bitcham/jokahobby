package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.TokenResponse;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.infra.security.jwt.CookieUtil;
import com.jokahobby.infra.security.jwt.JwtProperties;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.modules.account.RefreshToken;
import com.jokahobby.modules.account.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthApiController {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = "refreshToken") String refreshTokenRaw,
            HttpServletRequest httpRequest) {

        if (!jwtProvider.validateToken(refreshTokenRaw)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
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

    @PostMapping("/api/v1/auth/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenRaw,
            HttpServletRequest httpRequest) {

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
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return jwtProvider.getAccountId(token);
    }
}
