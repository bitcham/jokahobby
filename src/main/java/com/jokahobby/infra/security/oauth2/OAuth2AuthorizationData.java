package com.jokahobby.infra.security.oauth2;

import java.time.Instant;
import java.util.UUID;

public record OAuth2AuthorizationData(
        UUID accountId,
        boolean nicknameRequired,
        String deviceInfo,
        String ipAddress,
        String bindingHash,
        Instant createdAt
) {
}
