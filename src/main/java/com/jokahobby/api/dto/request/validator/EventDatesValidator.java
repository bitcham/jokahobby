package com.jokahobby.api.dto.request.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class EventDatesValidator implements ConstraintValidator<ValidEventDates, EventDateValidatable> {

    @Override
    public boolean isValid(EventDateValidatable value, ConstraintValidatorContext context) {
        if (value == null) return true;

        Instant now = Instant.now();
        Instant endEnrollment = value.endEnrollmentDateTime();
        Instant start = value.startDateTime();
        Instant end = value.endDateTime();

        if (endEnrollment == null || start == null || end == null) return true;

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (!endEnrollment.isAfter(now)) {
            context.buildConstraintViolationWithTemplate("End enrollment date must be after current time.")
                    .addPropertyNode("endEnrollmentDateTime").addConstraintViolation();
            valid = false;
        }

        if (!start.isAfter(now)) {
            context.buildConstraintViolationWithTemplate("Start date must be after current time.")
                    .addPropertyNode("startDateTime").addConstraintViolation();
            valid = false;
        }

        if (!end.isAfter(start)) {
            context.buildConstraintViolationWithTemplate("End date must be after start date.")
                    .addPropertyNode("endDateTime").addConstraintViolation();
            valid = false;
        }

        if (endEnrollment.isAfter(end)) {
            context.buildConstraintViolationWithTemplate("End enrollment date must be before end date.")
                    .addPropertyNode("endEnrollmentDateTime").addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
