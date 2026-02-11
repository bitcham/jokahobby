package com.jokahobby.api.dto.request;

import com.jokahobby.api.dto.request.validator.EventDateValidatable;
import com.jokahobby.api.dto.request.validator.ValidEventDates;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

@ValidEventDates
public record EventUpdateRequest(
        @NotBlank @Length(max = 50)
        String title,

        String description,

        @NotNull
        Instant endEnrollmentDateTime,

        @NotNull
        Instant startDateTime,

        @NotNull
        Instant endDateTime,

        @NotNull @Min(2)
        Integer limitOfEnrollments
) implements EventDateValidatable {
}
