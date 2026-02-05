package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ZoneRequest(
        @NotBlank String zoneName
) {
}
