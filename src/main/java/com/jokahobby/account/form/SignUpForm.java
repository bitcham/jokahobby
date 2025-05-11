package com.jokahobby.account.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class SignUpForm {

    @NotBlank
    @Length(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9가-힣äöåÄÖÅ]{3,20}$")
    private String nickname;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Length(min = 8, max = 50)
    private String password;

    @Override
    public String toString() {
        return "SignUpForm{" +
                "nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", password='********" + '\'' +
                '}';
    }

}
