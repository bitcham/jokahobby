package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.event.EventType;

import java.time.Instant;
import java.util.List;

public record EventResponse(
        Long id,
        String title,
        String description,
        EventType eventType,
        Instant createDateTime,
        Instant endEnrollmentDateTime,
        Instant startDateTime,
        Instant endDateTime,
        Integer limitOfEnrollments,
        long numberOfAcceptedEnrollments,
        int numberOfRemainSpots,
        boolean isExpired,
        boolean isEnrollable,
        boolean isDisenrollable,
        boolean enrolled,
        MemberResponse createdBy,
        List<EnrollmentResponse> enrollments
) {
    public static EventResponse from(Event event, Account account) {
        boolean isEnrollable = account != null && event.isEnrollableFor(account);
        boolean isDisenrollable = account != null && event.isDisenrollableFor(account);
        boolean enrolled = account != null && event.getEnrollments().stream()
                .anyMatch(e -> e.getAccount().equals(account));

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventType(),
                event.getCreateDateTime(),
                event.getEndEnrollmentDateTime(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getLimitOfEnrollments(),
                event.getNumberOfAcceptedEnrollments(),
                event.numberOfRemainSpots(),
                event.getEndDateTime().isBefore(Instant.now()),
                isEnrollable,
                isDisenrollable,
                enrolled,
                MemberResponse.from(event.getCreatedBy()),
                event.getEnrollments().stream().map(EnrollmentResponse::from).toList()
        );
    }
}
