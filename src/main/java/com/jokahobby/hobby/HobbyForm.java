package com.jokahobby.hobby;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class HobbyForm {

    @NotBlank
    @Length(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9가-힣äöåÄÖÅ_-]{3,20}$")
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
