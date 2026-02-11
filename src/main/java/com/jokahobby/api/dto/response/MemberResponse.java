package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;

public record MemberResponse(
        String nickname,
        String profileImage
) {
    public static MemberResponse from(Account account) {
        return new MemberResponse(account.getNickname(), account.getProfileImage());
    }
}
