package com.jokahobby.modules.event;

import com.jokahobby.modules.hobby.Hobby;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>{

    @EntityGraph(value = "Event.withEnrollments", type = EntityGraph.EntityGraphType.LOAD)
    List<Event> findByHobbyOrderByStartDateTime(Hobby hobby);

    @EntityGraph(value = "Event.withEnrollments", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Event> findWithEnrollmentsById(Long id);
}
