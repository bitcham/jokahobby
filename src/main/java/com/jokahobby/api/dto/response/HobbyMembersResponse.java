package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;

import java.util.List;

public record HobbyMembersResponse(
        MemberResponse host,
        List<MemberResponse> managers,
        List<MemberResponse> members
) {
    public static HobbyMembersResponse from(Account host, List<Account> managers, List<Account> members) {
        return new HobbyMembersResponse(
                MemberResponse.from(host, "HOST"),
                managers.stream().map(m -> MemberResponse.from(m, "MANAGER")).toList(),
                members.stream().map(m -> MemberResponse.from(m, "MEMBER")).toList()
        );
    }
}
