package com.jokahobby.api.dto.response;

import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.tag.Tag;

import java.time.Instant;
import java.util.List;

public record HobbyListResponse(
        Long id,
        String path,
        String title,
        String shortDescription,
        String image,
        boolean recruiting,
        int memberCount,
        List<TagResponse> tags,
        Instant publishedDateTime
) {
    public static HobbyListResponse from(Hobby hobby, List<Tag> tags) {
        return new HobbyListResponse(
                hobby.getId(),
                hobby.getPath(),
                hobby.getTitle(),
                hobby.getShortDescription(),
                hobby.getImage(),
                hobby.isRecruiting(),
                hobby.getMemberCount(),
                tags.stream().map(TagResponse::from).toList(),
                hobby.getPublishedDateTime()
        );
    }
}
