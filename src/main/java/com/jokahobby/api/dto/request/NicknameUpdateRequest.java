package com.jokahobby.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record NicknameUpdateRequest(
        @NotBlank
        @Length(min = 3, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9가-힣äöåÄÖÅ]{3,20}$", message = "Nickname must be 3-20 characters (letters, numbers, Korean, Nordic)")
        String nickname
) {
}
