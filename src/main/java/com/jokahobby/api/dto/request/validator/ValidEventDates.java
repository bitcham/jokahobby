package com.jokahobby.api.dto.request.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EventDatesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventDates {
    String message() default "Event date constraints are invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
