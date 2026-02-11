package com.jokahobby.modules.hobby.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class HobbyForm {

    public static final String VALID_PATH_PATTERN = "^[a-zA-Z0-9äöåÄÖÅ_-]{3,20}$";

    @NotBlank
    @Length(min = 3, max = 20)
    @Pattern(regexp = VALID_PATH_PATTERN)
    private String path;

    @NotBlank
    @Length(max = 50)
    private String title;

    @NotBlank
    @Length(max = 150)
    private String shortDescription;

    @NotBlank
    @Length(max = 500)
    private String fullDescription;

}
