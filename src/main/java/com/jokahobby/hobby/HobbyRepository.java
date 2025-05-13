package com.jokahobby.hobby;

import com.jokahobby.domain.Hobby;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface HobbyRepository extends JpaRepository<Hobby, Long> {

    boolean existsByPath(String path);

    boolean existsByTitle(String title);

    @EntityGraph(value = "Hobby.withAll", type = EntityGraph.EntityGraphType.LOAD)
    Hobby findByPath(String path);

    @EntityGraph(value = "Hobby.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findAccountWithTagsByPath(String path);

    @EntityGraph(value = "Hobby.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findAccountWithZonesByPath(String path);

    @EntityGraph(value = "Hobby.withManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findHobbyWithManagersByPath(String path);
}
