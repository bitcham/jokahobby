package com.jokahobby.modules.event.event;

import com.jokahobby.modules.event.Enrollment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EnrollmentEvent {

    protected final Enrollment enrollment;

    protected final String message;
}
