package com.jokahobby.modules.hobby;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface HobbyRepositoryExtension {
    List<Hobby> findByKeyword(String keyword);
}
