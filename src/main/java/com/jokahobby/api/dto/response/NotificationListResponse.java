package com.jokahobby.api.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> notifications,
        NotificationCountResponse counts
) {
}
