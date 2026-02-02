package com.jokahobby.infra.security.jwt;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {

    private CookieUtil() {
    }

    public static ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAgeMs, boolean secure) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    public static ResponseCookie deleteRefreshTokenCookie(boolean secure) {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }
}
