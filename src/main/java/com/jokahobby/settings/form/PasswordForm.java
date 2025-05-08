package com.jokahobby.settings.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class PasswordForm {

    @Length(min = 8, max = 50)
    private String newPassword;

    private String newPasswordConfirm;
}
