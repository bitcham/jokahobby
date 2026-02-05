package com.jokahobby.api.dto.response;

import com.jokahobby.modules.tag.Tag;

public record TagResponse(
        Long id,
        String title
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getTitle());
    }
}
