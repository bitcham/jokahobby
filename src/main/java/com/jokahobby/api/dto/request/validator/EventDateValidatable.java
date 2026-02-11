package com.jokahobby.api.dto.request.validator;

import java.time.Instant;

public interface EventDateValidatable {
    Instant endEnrollmentDateTime();
    Instant startDateTime();
    Instant endDateTime();
}
