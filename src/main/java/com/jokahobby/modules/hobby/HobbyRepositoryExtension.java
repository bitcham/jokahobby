package com.jokahobby.modules.hobby;

import com.jokahobby.modules.hobby.HobbySortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface HobbyRepositoryExtension {
    Page<Hobby> findByKeyword(String keyword, Pageable pageable);
    Page<Hobby> findPublished(String country, String city, HobbySortType sort, Pageable pageable);
}
