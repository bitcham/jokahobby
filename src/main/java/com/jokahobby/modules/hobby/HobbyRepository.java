package com.jokahobby.modules.hobby;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryExtension {

    boolean existsByPath(String path);

    boolean existsByTitle(String title);

    Hobby findByPath(String path);

    Hobby findHobbyOnlyByPath(String path);
}
