package com.jokahobby.infra.security;

import tools.jackson.databind.ObjectMapper;
import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.infra.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityExceptionHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }
}
