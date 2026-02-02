package com.jokahobby.api.dto.response;

import java.util.Map;

public record ErrorDetail(
        String code,
        String message,
        Map<String, String> fieldErrors
) {
}
