package com.jokahobby.modules.hobby;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Transactional(readOnly = true)
public interface HobbyRepositoryExtension {
    Page<Hobby> findByKeyword(String keyword, Pageable pageable);
    List<Hobby> findByAccount(Set<Tag> tags, Set<Zone> zones);
}
