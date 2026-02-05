package com.jokahobby.api.dto.request;

public record NotificationUpdateRequest(
        boolean hobbyCreatedByEmail,
        boolean hobbyCreatedByWeb,
        boolean hobbyEnrollmentResultByEmail,
        boolean hobbyEnrollmentResultByWeb,
        boolean hobbyUpdatedByEmail,
        boolean hobbyUpdatedByWeb
) {
}
