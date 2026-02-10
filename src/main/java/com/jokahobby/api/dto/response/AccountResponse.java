package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String nickname,
        String provider,
        String bio,
        String url,
        String location,
        String profileImage,
        Instant joinedAt,
        boolean hobbyCreatedByEmail,
        boolean hobbyCreatedByWeb,
        boolean hobbyEnrollmentResultByEmail,
        boolean hobbyEnrollmentResultByWeb,
        boolean hobbyUpdatedByEmail,
        boolean hobbyUpdatedByWeb
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getNickname(),
                account.getProvider(),
                account.getBio(),
                account.getUrl(),
                account.getLocation(),
                account.getProfileImage(),
                account.getJoinedAt(),
                account.isHobbyCreatedByEmail(),
                account.isHobbyCreatedByWeb(),
                account.isHobbyEnrollmentResultByEmail(),
                account.isHobbyEnrollmentResultByWeb(),
                account.isHobbyUpdatedByEmail(),
                account.isHobbyUpdatedByWeb()
        );
    }
}
