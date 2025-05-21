package com.jokahobby.modules.event.event;

import com.jokahobby.modules.event.Enrollment;


public class EnrollmentRejectedEvent extends EnrollmentEvent {
    public EnrollmentRejectedEvent(Enrollment enrollment) {
        super(enrollment, "Your participation request has been rejected.");
    }
}
