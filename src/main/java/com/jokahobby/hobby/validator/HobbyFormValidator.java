package com.jokahobby.hobby.validator;

import com.jokahobby.hobby.HobbyForm;
import com.jokahobby.hobby.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class HobbyFormValidator implements Validator {

    private final HobbyRepository hobbyRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return HobbyForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        HobbyForm hobbyForm = (HobbyForm) target;
        if (hobbyRepository.existsByPath(hobbyForm.getPath())) {
            errors.rejectValue("path", "invalid.path","The path already exists.");
        }

        if(hobbyRepository.existsByTitle(hobbyForm.getTitle())) {
            errors.rejectValue("title", "invalid.title","The title already exists.");
        }
    }
}
