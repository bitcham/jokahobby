package com.jokahobby.modules.event.validator;

import com.jokahobby.modules.event.form.EventForm;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;


public class EventValidator implements Validator {


    @Override
    public boolean supports(Class<?> clazz) {
        return EventForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        EventForm eventForm = (EventForm) target;

        if(eventForm.getLimitOfEnrollments() < 2) {
            errors.rejectValue("limitOfEnrollments", "invalid.limitOfEnrollments", "Limit of enrollments must be at least 2.");
        }

        if(isNotValidEndEnrollmentDateTime(eventForm)) {
            errors.rejectValue("endEnrollmentDateTime", "invalid.endEnrollmentDateTime", "End enrollment date time must be after current date time and before end date time.");
        }

        if(IsNotValidEndDateTime(eventForm)) {
            errors.rejectValue("endDateTime", "invalid.endDateTime", "End date time must be after start date time.");
        }

        if(isNotValidStartDateTime(eventForm)) {
            errors.rejectValue("startDateTime", "invalid.startDateTime", "Start date time must be after current date time.");
        }
    }

    private static boolean isNotValidStartDateTime(EventForm eventForm) {
        return eventForm.getStartDateTime().isBefore(LocalDateTime.now());
    }

    private static boolean isNotValidEndEnrollmentDateTime(EventForm eventForm) {
        return eventForm.getEndEnrollmentDateTime().isBefore(LocalDateTime.now()) || eventForm.getEndEnrollmentDateTime().isAfter(eventForm.getEndDateTime());
    }

    private static boolean IsNotValidEndDateTime(EventForm eventForm) {
        return eventForm.getEndDateTime().isBefore(eventForm.getStartDateTime()) || eventForm.getEndDateTime().isBefore(eventForm.getEndEnrollmentDateTime());
    }


}
