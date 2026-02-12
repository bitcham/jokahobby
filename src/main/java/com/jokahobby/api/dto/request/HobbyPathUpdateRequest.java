package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import static com.jokahobby.modules.hobby.Hobby.VALID_PATH_PATTERN;

public record HobbyPathUpdateRequest(
        @NotBlank @Length(min = 3, max = 20) @Pattern(regexp = VALID_PATH_PATTERN)
        String newPath
) {
}
