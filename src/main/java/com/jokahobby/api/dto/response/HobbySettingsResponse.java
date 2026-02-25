package com.jokahobby.api.dto.response;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;

import java.time.Instant;
import java.util.List;

public record HobbySettingsResponse(
        Long id,
        String path,
        String title,
        String shortDescription,
        String fullDescription,
        String image,
        boolean useBanner,
        boolean published,
        boolean closed,
        boolean recruiting,
        int memberCount,
        Instant publishedDateTime,
        Instant closedDateTime,
        Instant recruitingUpdatedDateTime,
        List<TagResponse> tags,
        List<ZoneResponse> zones,
        MemberResponse host,
        List<MemberResponse> managers,
        List<MemberResponse> members
) {
    public static HobbySettingsResponse from(Hobby hobby, List<Tag> tags, List<Zone> zones,
                                              Account host, List<Account> managers, List<Account> members) {
        return new HobbySettingsResponse(
                hobby.getId(),
                hobby.getPath(),
                hobby.getTitle(),
                hobby.getShortDescription(),
                hobby.getFullDescription(),
                hobby.getImage(),
                hobby.isUseBanner(),
                hobby.isPublished(),
                hobby.isClosed(),
                hobby.isRecruiting(),
                hobby.getMemberCount(),
                hobby.getPublishedDateTime(),
                hobby.getClosedDateTime(),
                hobby.getRecruitingUpdatedDateTime(),
                tags.stream().map(TagResponse::from).toList(),
                zones.stream().map(ZoneResponse::from).toList(),
                MemberResponse.from(host, "HOST"),
                managers.stream().map(m -> MemberResponse.from(m, "MANAGER")).toList(),
                members.stream().map(m -> MemberResponse.from(m, "MEMBER")).toList()
        );
    }
}
