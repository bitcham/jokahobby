package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HostTransferRequest(@NotBlank String nickname) {
}
