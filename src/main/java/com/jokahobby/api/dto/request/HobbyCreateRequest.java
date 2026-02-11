package com.jokahobby.api.dto.request;

import com.jokahobby.modules.hobby.Hobby;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import static com.jokahobby.modules.hobby.form.HobbyForm.VALID_PATH_PATTERN;

public record HobbyCreateRequest(
        @NotBlank @Length(min = 3, max = 20) @Pattern(regexp = VALID_PATH_PATTERN)
        String path,

        @NotBlank @Length(max = 50)
        String title,

        @NotBlank @Length(max = 150)
        String shortDescription,

        @NotBlank @Length(max = 500)
        String fullDescription
) {
    public Hobby toEntity() {
        return Hobby.builder()
                .path(path)
                .title(title)
                .shortDescription(shortDescription)
                .fullDescription(fullDescription)
                .build();
    }
}
