package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;

import java.time.Instant;

public record ProfileResponse(
        String nickname,
        String bio,
        String url,
        String location,
        String profileImage,
        Instant joinedAt
) {
    public static ProfileResponse from(Account account) {
        return new ProfileResponse(
                account.getNickname(),
                account.getBio(),
                account.getUrl(),
                account.getLocation(),
                account.getProfileImage(),
                account.getJoinedAt()
        );
    }
}
