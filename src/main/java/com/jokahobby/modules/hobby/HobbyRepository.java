package com.jokahobby.modules.hobby;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryExtension {

    boolean existsByPath(String path);

    boolean existsByTitle(String title);

    Optional<Hobby> findByPath(String path);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Hobby h WHERE h.path = :path")
    Optional<Hobby> findByPathForUpdate(@Param("path") String path);
}
