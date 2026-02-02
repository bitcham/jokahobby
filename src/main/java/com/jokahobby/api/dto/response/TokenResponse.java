package com.jokahobby.api.dto.response;

public record TokenResponse(
        String accessToken,
        long expiresIn
) {
}
