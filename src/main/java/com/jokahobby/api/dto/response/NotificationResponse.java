package com.jokahobby.api.dto.response;

import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String link,
        String message,
        NotificationType notificationType,
        boolean checked,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getLink(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.isChecked(),
                notification.getCreatedAt()
        );
    }
}
