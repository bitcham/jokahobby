package com.jokahobby.api.dto.response;

import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.event.EventType;

import java.time.Instant;

public record EventListResponse(
        Long id,
        String title,
        EventType eventType,
        Instant startDateTime,
        Instant endDateTime,
        Instant endEnrollmentDateTime,
        Integer limitOfEnrollments,
        long numberOfAcceptedEnrollments,
        int numberOfRemainSpots,
        boolean isExpired,
        MemberResponse createdBy
) {
    public static EventListResponse from(Event event) {
        return new EventListResponse(
                event.getId(),
                event.getTitle(),
                event.getEventType(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getEndEnrollmentDateTime(),
                event.getLimitOfEnrollments(),
                event.getNumberOfAcceptedEnrollments(),
                event.numberOfRemainSpots(),
                event.getEndDateTime().isBefore(Instant.now()),
                MemberResponse.from(event.getCreatedBy())
        );
    }
}
