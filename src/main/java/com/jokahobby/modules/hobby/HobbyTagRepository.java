package com.jokahobby.modules.hobby;

import com.jokahobby.modules.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HobbyTagRepository extends JpaRepository<HobbyTag, Long> {

    List<HobbyTag> findAllByHobbyId(Long hobbyId);

    Optional<HobbyTag> findByHobbyAndTag(Hobby hobby, Tag tag);

    void deleteByHobbyAndTag(Hobby hobby, Tag tag);

    boolean existsByHobbyAndTag(Hobby hobby, Tag tag);
}
