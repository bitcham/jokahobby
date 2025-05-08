package com.jokahobby.settings.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class NicknameForm {
    @NotBlank
    @Length(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$")
    private String nickname;
}
