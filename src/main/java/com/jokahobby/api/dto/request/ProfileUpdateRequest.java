package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 50) String bio,
        @Size(max = 50) String url,
        String location,
        String profileImage
) {
}
