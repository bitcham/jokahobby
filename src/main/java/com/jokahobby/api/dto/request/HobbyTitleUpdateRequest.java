package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record HobbyTitleUpdateRequest(
        @NotBlank @Length(max = 50)
        String newTitle
) {
}
