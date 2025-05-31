package com.jokahobby.modules.hobby;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface HobbyRepositoryExtension {
    Page<Hobby> findByKeyword(String keyword, Pageable pageable);
}
