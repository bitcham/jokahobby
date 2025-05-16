package com.jokahobby.modules.hobby.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
public class HobbyDescriptionForm {

    @NotBlank
    @Length(max = 150)
    private String shortDescription;

    @NotBlank
    @Length(max = 500)
    private String fullDescription;

}
