package com.jokahobby.modules.event.event;

import com.jokahobby.modules.event.Enrollment;


public class EnrollmentAcceptedEvent extends EnrollmentEvent {
    public EnrollmentAcceptedEvent(Enrollment enrollment) {
        super(enrollment, "Your participation request has been confirmed. Please attend the meeting.");
    }
}
