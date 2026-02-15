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
    INVALID_AUTHORIZATION_CODE("AUTH_008", "Invalid or expired authorization code.", 401),

    // Authorization
    FORBIDDEN("AUTH_010", "Access denied.", 403),

    // Validation
    INVALID_INPUT("VALID_001", "Invalid input value.", 400),
    DUPLICATE_NICKNAME("VALID_003", "Nickname already exists.", 409),
    DATA_CONFLICT("VALID_004", "Data conflict.", 409),

    // Hobby
    HOBBY_ALREADY_PUBLISHED("HOBBY_001", "Hobby is already published or closed.", 400),
    HOBBY_NOT_PUBLISHED("HOBBY_002", "Hobby is already closed or not published.", 400),
    HOBBY_RECRUIT_COOLDOWN("HOBBY_003", "Cannot update recruiting. Please try again after one hour.", 400),
    HOBBY_NOT_REMOVABLE("HOBBY_004", "Hobby cannot be removed.", 400),
    HOBBY_NOT_JOINABLE("HOBBY_005", "Cannot join this hobby.", 400),
    HOBBY_NOT_MEMBER("HOBBY_006", "Not a member of this hobby.", 400),
    HOBBY_PATH_ALREADY_EXISTS("HOBBY_007", "Hobby path already exists.", 409),
    HOBBY_TITLE_ALREADY_EXISTS("HOBBY_008", "Hobby title already exists.", 409),
    INVALID_HOBBY_PATH("HOBBY_009", "Invalid hobby path format.", 400),

    // Event
    EVENT_NOT_ENROLLABLE("EVENT_001", "Cannot enroll in this event.", 400),
    EVENT_NOT_DISENROLLABLE("EVENT_002", "Cannot disenroll from this event.", 400),
    EVENT_ENROLLMENT_LIMIT_TOO_LOW("EVENT_003", "Enrollment limit cannot be less than current accepted count.", 400),
    EVENT_CANNOT_ACCEPT("EVENT_004", "Cannot accept this enrollment.", 400),
    EVENT_CANNOT_REJECT("EVENT_005", "Cannot reject this enrollment.", 400),
    EVENT_INVALID_DATES("EVENT_006", "Event date constraints are invalid.", 400),

    // Resource
    ACCOUNT_NOT_FOUND("RESOURCE_001", "Account not found.", 404),
    HOBBY_NOT_FOUND("RESOURCE_002", "Hobby not found.", 404),
    EVENT_NOT_FOUND("RESOURCE_003", "Event not found.", 404),
    ENROLLMENT_NOT_FOUND("RESOURCE_004", "Enrollment not found.", 404),
    NOTIFICATION_NOT_FOUND("RESOURCE_005", "Notification not found.", 404),

    // Server
    INTERNAL_SERVER_ERROR("SERVER_001", "Internal server error.", 500);

    private final String code;
    private final String message;
    private final int status;
}
