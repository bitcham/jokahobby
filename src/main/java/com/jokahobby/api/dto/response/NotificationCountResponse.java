package com.jokahobby.api.dto.response;

import com.jokahobby.modules.notification.NotificationType;

import java.util.Map;

public record NotificationCountResponse(
        long total,
        long unchecked,
        long checked,
        Map<NotificationType, Long> byType
) {
}
