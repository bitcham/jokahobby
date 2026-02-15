package com.jokahobby.api.dto.response;

public record OAuth2TokenResponse(
        String accessToken,
        long expiresIn,
        boolean nicknameRequired
) {
}
