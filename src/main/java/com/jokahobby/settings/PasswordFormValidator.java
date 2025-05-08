package com.jokahobby.settings;


import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class PasswordFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PasswordForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PasswordForm passwordForm = (PasswordForm) target;

        if (!passwordForm.getNewPassword().equals(passwordForm.getNewPasswordConfirm())) {
            errors.rejectValue("newPasswordConfirm", "invalid.password", "New passwords do not match.");
        }
    }
}
