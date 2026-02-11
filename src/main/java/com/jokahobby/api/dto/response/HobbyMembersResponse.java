package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;

import java.util.List;

public record HobbyMembersResponse(
        List<MemberResponse> managers,
        List<MemberResponse> members
) {
    public static HobbyMembersResponse from(List<Account> managers, List<Account> members) {
        return new HobbyMembersResponse(
                managers.stream().map(MemberResponse::from).toList(),
                members.stream().map(MemberResponse::from).toList()
        );
    }
}
