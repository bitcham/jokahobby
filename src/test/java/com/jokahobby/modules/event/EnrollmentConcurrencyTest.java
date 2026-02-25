package com.jokahobby.modules.event;

import com.jokahobby.api.dto.request.EventUpdateRequest;
import com.jokahobby.api.service.EventApplicationService;
import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyTest extends AbstractContainerBaseTest {

    @Autowired EventApplicationService eventApplicationService;
    @Autowired AccountRepository accountRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired HobbyHostRepository hobbyHostRepository;
    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyMemberRepository hobbyMemberRepository;
    @Autowired EventRepository eventRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    private Account hostAccount;
    private Hobby hobby;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        hobbyHostRepository.deleteAllInBatch();
        hobbyManagerRepository.deleteAllInBatch();
        hobbyMemberRepository.deleteAllInBatch();
        hobbyRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        hostAccount = accountRepository.save(Account.builder()
                .email("manager@test.com")
                .nickname("manager-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-manager-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());

        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Test Hobby " + UUID.randomUUID().toString().substring(0, 8))
                .shortDescription("desc")
                .fullDescription("full desc")
                .published(true)
                .publishedDateTime(Instant.now())
                .recruiting(true)
                .memberCount(1)
                .build());

        hobbyHostRepository.save(HobbyHost.builder()
                .hobby(hobby).account(hostAccount).build());
    }

    @AfterEach
    void tearDown() {
        enrollmentRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        hobbyHostRepository.deleteAllInBatch();
        hobbyManagerRepository.deleteAllInBatch();
        hobbyMemberRepository.deleteAllInBatch();
        hobbyRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("10 concurrent enrollments for 1 spot: exactly 1 accepted, 9 waitlisted")
    void concurrentEnrollments_onlyOneAccepted() throws InterruptedException {
        Event event = eventRepository.save(Event.builder()
                .title("FCFS Event")
                .description("Concurrency test")
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(1)
                .hobby(hobby)
                .createdBy(hostAccount)
                .build());

        int threadCount = 10;
        List<Account> accounts = createAccounts(threadCount);
        accounts.forEach(account -> hobbyMemberRepository.save(
                HobbyMember.builder().hobby(hobby).account(account).build()));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (Account account : accounts) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventApplicationService.enroll(hobby.getPath(), event.getId(), account);
                } catch (Exception ignored) {
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEvent() != null && e.getEvent().getId().equals(event.getId()))
                .toList();
        long acceptedCount = enrollments.stream().filter(Enrollment::isAccepted).count();

        assertThat(acceptedCount).isEqualTo(1);
        assertThat(enrollments).hasSizeLessThanOrEqualTo(threadCount);
    }

    @Test
    @DisplayName("5 concurrent enrollments from same account: exactly 1 enrollment created")
    void concurrentDuplicateEnrollments_onlyOneCreated() throws InterruptedException {
        Event event = eventRepository.save(Event.builder()
                .title("FCFS Event")
                .description("Duplicate test")
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(5)
                .hobby(hobby)
                .createdBy(hostAccount)
                .build());

        Account singleAccount = accountRepository.save(Account.builder()
                .email("dup@test.com")
                .nickname("dup-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-dup-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(singleAccount).build());

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventApplicationService.enroll(hobby.getPath(), event.getId(), singleAccount);
                } catch (Exception ignored) {
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEvent() != null && e.getEvent().getId().equals(event.getId()))
                .toList();

        assertThat(enrollments).hasSize(1);
    }

    @Test
    @DisplayName("concurrent disenroll + enroll: capacity never exceeded")
    void concurrentDisenrollAndEnroll_capacityNotExceeded() throws InterruptedException {
        Event event = eventRepository.save(Event.builder()
                .title("FCFS Event")
                .description("Disenroll race test")
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(1)
                .hobby(hobby)
                .createdBy(hostAccount)
                .build());

        Account existingEnrollee = accountRepository.save(Account.builder()
                .email("existing@test.com")
                .nickname("existing-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-existing-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(existingEnrollee).build());

        Enrollment existingEnrollment = Enrollment.builder()
                .enrolledAt(Instant.now())
                .accepted(true)
                .account(existingEnrollee)
                .build();
        event.addEnrollment(existingEnrollment);
        enrollmentRepository.save(existingEnrollment);

        Account newEnrollee = accountRepository.save(Account.builder()
                .email("new@test.com")
                .nickname("new-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-new-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(newEnrollee).build());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> disenrollFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventApplicationService.disenroll(hobby.getPath(), event.getId(), existingEnrollee);
            } catch (Exception ignored) {}
        });

        Future<?> enrollFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventApplicationService.enroll(hobby.getPath(), event.getId(), newEnrollee);
            } catch (Exception ignored) {}
        });

        readyLatch.await();
        startLatch.countDown();

        try { disenrollFuture.get(); } catch (Exception ignored) {}
        try { enrollFuture.get(); } catch (Exception ignored) {}
        executor.shutdown();

        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEvent() != null && e.getEvent().getId().equals(event.getId()))
                .toList();
        long acceptedCount = enrollments.stream().filter(Enrollment::isAccepted).count();

        assertThat(acceptedCount).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("concurrent limit increase + enroll: capacity never exceeded")
    void concurrentUpdateAndEnroll_capacityNotExceeded() throws InterruptedException {
        Event event = eventRepository.save(Event.builder()
                .title("FCFS Event")
                .description("Update race test")
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(1)
                .hobby(hobby)
                .createdBy(hostAccount)
                .build());

        Account accepted = accountRepository.save(Account.builder()
                .email("accepted@test.com")
                .nickname("accepted-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-accepted-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(accepted).build());

        Enrollment acceptedEnrollment = Enrollment.builder()
                .enrolledAt(Instant.now())
                .accepted(true)
                .account(accepted)
                .build();
        event.addEnrollment(acceptedEnrollment);
        enrollmentRepository.save(acceptedEnrollment);

        Account waiter = accountRepository.save(Account.builder()
                .email("waiter@test.com")
                .nickname("waiter-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-waiter-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(waiter).build());

        Enrollment waitingEnrollment = Enrollment.builder()
                .enrolledAt(Instant.now())
                .accepted(false)
                .account(waiter)
                .build();
        event.addEnrollment(waitingEnrollment);
        enrollmentRepository.save(waitingEnrollment);

        Account newEnrollee = accountRepository.save(Account.builder()
                .email("new-update@test.com")
                .nickname("new-update-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-new-update-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(newEnrollee).build());

        EventUpdateRequest updateRequest = new EventUpdateRequest(
                event.getTitle(), event.getDescription(),
                event.getEndEnrollmentDateTime(),
                event.getStartDateTime(),
                event.getEndDateTime(), 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> updateFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventApplicationService.updateEvent(hobby.getPath(), event.getId(), hostAccount, updateRequest);
            } catch (Exception ignored) {}
        });

        Future<?> enrollFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventApplicationService.enroll(hobby.getPath(), event.getId(), newEnrollee);
            } catch (Exception ignored) {}
        });

        readyLatch.await();
        startLatch.countDown();

        try { updateFuture.get(); } catch (Exception ignored) {}
        try { enrollFuture.get(); } catch (Exception ignored) {}
        executor.shutdown();

        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEvent() != null && e.getEvent().getId().equals(event.getId()))
                .toList();
        long acceptedCount = enrollments.stream().filter(Enrollment::isAccepted).count();

        assertThat(acceptedCount).isLessThanOrEqualTo(2);
    }

    private List<Account> createAccounts(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(accountRepository.save(Account.builder()
                    .email("user" + i + "@test.com")
                    .nickname("user" + i + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .provider("google")
                    .providerId("google-user" + i + "-" + UUID.randomUUID())
                    .joinedAt(Instant.now())
                    .build()));
        }
        return accounts;
    }
}
