package com.jokahobby.infra.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication
    UNAUTHORIZED("AUTH_001", "Authentication is required.", 401),
    INVALID_TOKEN("AUTH_002", "Invalid or expired token.", 401),
    REFRESH_TOKEN_EXPIRED("AUTH_004", "Refresh token has expired.", 401),
    REFRESH_TOKEN_REUSED("AUTH_005", "Refresh token has already been used.", 401),
    MAX_SESSIONS_EXCEEDED("AUTH_006", "Maximum session limit reached.", 401),
    OAUTH2_AUTHENTICATION_FAILED("AUTH_007", "OAuth2 authentication failed.", 401),

    // Authorization
    FORBIDDEN("AUTH_010", "Access denied.", 403),

    // Validation
    INVALID_INPUT("VALID_001", "Invalid input value.", 400),
    DUPLICATE_NICKNAME("VALID_003", "Nickname already exists.", 409),

    // Hobby
    HOBBY_ALREADY_PUBLISHED("HOBBY_001", "Hobby is already published or closed.", 400),
    HOBBY_NOT_PUBLISHED("HOBBY_002", "Hobby is already closed or not published.", 400),
    HOBBY_RECRUIT_COOLDOWN("HOBBY_003", "Cannot update recruiting. Please try again after one hour.", 400),
    HOBBY_NOT_REMOVABLE("HOBBY_004", "Hobby cannot be removed.", 400),

    // Resource
    ACCOUNT_NOT_FOUND("RESOURCE_001", "Account not found.", 404),
    HOBBY_NOT_FOUND("RESOURCE_002", "Hobby not found.", 404),
    EVENT_NOT_FOUND("RESOURCE_003", "Event not found.", 404),

    // Server
    INTERNAL_SERVER_ERROR("SERVER_001", "Internal server error.", 500);

    private final String code;
    private final String message;
    private final int status;
}
