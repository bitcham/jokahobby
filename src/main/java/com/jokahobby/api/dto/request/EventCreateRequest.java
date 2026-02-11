package com.jokahobby.api.dto.request;

import com.jokahobby.api.dto.request.validator.EventDateValidatable;
import com.jokahobby.api.dto.request.validator.ValidEventDates;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.event.EventType;
import com.jokahobby.modules.hobby.Hobby;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

@ValidEventDates
public record EventCreateRequest(
        @NotBlank @Length(max = 50)
        String title,

        String description,

        @NotNull
        EventType eventType,

        @NotNull
        Instant endEnrollmentDateTime,

        @NotNull
        Instant startDateTime,

        @NotNull
        Instant endDateTime,

        @NotNull @Min(2)
        Integer limitOfEnrollments
) implements EventDateValidatable {

    public Event toEntity(Hobby hobby, Account createdBy) {
        return Event.builder()
                .title(title)
                .description(description)
                .eventType(eventType)
                .endEnrollmentDateTime(endEnrollmentDateTime)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .limitOfEnrollments(limitOfEnrollments)
                .hobby(hobby)
                .createdBy(createdBy)
                .build();
    }
}
