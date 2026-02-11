package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record HobbyDescriptionUpdateRequest(
        @NotBlank @Length(max = 150)
        String shortDescription,

        @NotBlank @Length(max = 500)
        String fullDescription
) {
}
