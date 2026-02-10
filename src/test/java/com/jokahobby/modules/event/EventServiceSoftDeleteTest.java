package com.jokahobby.modules.event;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class EventServiceSoftDeleteTest extends AbstractContainerBaseTest {

    @Autowired EventService eventService;
    @Autowired EventRepository eventRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired EntityManager em;

    private Hobby hobby;
    private Account account;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        eventRepository.deleteAll();
        hobby = hobbyRepository.save(Hobby.builder()
                .path("event-hobby")
                .title("Event Hobby")
                .shortDescription("desc")
                .published(true)
                .build());
        account = accountRepository.save(Account.builder()
                .email("event-user@test.com")
                .nickname("eventuser")
                .provider("google")
                .providerId("google-event")
                .joinedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("deleteEvent() soft-deletes event and its enrollments")
    void deleteEventSoftDeletesEventAndEnrollments() {
        Event event = new Event();
        event.setTitle("Test Event");
        event.setCreatedBy(account);
        event.setCreatedDateTime(Instant.now());
        event.setEndEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)));
        event.setStartDateTime(Instant.now().plus(Duration.ofDays(8)));
        event.setEndDateTime(Instant.now().plus(Duration.ofDays(9)));
        event.setLimitOfEnrollments(10);
        event.setEventType(EventType.FCFS);
        event.setHobby(hobby);
        event = eventRepository.save(event);

        Enrollment enrollment = new Enrollment();
        enrollment.setAccount(account);
        enrollment.setEnrolledAt(Instant.now());
        enrollment.setAccepted(true);
        event.addEnrollment(enrollment);
        enrollmentRepository.save(enrollment);
        em.flush();

        eventService.deleteEvent(event);
        em.flush();
        em.clear();

        // Event is soft-deleted
        assertThat(eventRepository.findById(event.getId())).isEmpty();

        // Enrollments are also soft-deleted (@SQLRestriction filters them)
        assertThat(enrollmentRepository.count()).isZero();
    }
}
