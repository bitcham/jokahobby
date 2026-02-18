package com.jokahobby.modules.hobby;

import com.jokahobby.modules.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HobbyTagRepository extends JpaRepository<HobbyTag, Long> {

    @Query("SELECT ht FROM HobbyTag ht JOIN FETCH ht.tag WHERE ht.hobby.id = :hobbyId")
    List<HobbyTag> findAllByHobbyId(Long hobbyId);

    @Query("SELECT ht FROM HobbyTag ht JOIN FETCH ht.tag WHERE ht.hobby.id IN :hobbyIds")
    List<HobbyTag> findAllByHobbyIdIn(@Param("hobbyIds") Collection<Long> hobbyIds);

    Optional<HobbyTag> findByHobbyAndTag(Hobby hobby, Tag tag);

    void deleteByHobbyAndTag(Hobby hobby, Tag tag);

    boolean existsByHobbyAndTag(Hobby hobby, Tag tag);
}
