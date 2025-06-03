package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByEventAndAccount(Event event, Account account);

    Enrollment findByEventAndAccount(Event event, Account account);

    List<Enrollment> findByEvent(Event event);

    @EntityGraph("Enrollment.withEventAndHobby")
    List<Enrollment> findByAccountAndAcceptedOrderByEnrolledAtDesc(Account accountLoaded, boolean b);
}
