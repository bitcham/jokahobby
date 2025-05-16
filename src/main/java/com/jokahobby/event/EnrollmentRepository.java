package com.jokahobby.event;

import com.jokahobby.domain.Account;
import com.jokahobby.domain.Enrollment;
import com.jokahobby.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByEventAndAccount(Event event, Account account);

    Enrollment findByEventAndAccount(Event event, Account account);
}
