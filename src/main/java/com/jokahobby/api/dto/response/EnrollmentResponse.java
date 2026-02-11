package com.jokahobby.api.dto.response;

import com.jokahobby.modules.event.Enrollment;

import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        MemberResponse account,
        Instant enrolledAt,
        boolean accepted,
        boolean attended
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                MemberResponse.from(enrollment.getAccount()),
                enrollment.getEnrolledAt(),
                enrollment.isAccepted(),
                enrollment.isAttended()
        );
    }
}
