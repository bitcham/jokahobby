package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuth2CodeRequest(
        @NotBlank String code
) {
}
