package com.jokahobby.modules.hobby;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryExtension {

    boolean existsByPath(String path);

    boolean existsByTitle(String title);

    Optional<Hobby> findByPath(String path);
}
