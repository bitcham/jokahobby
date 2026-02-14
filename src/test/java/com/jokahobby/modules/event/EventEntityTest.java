package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventEntityTest {

    @Test
    @DisplayName("acceptNextWaitingEnrollment: FCFS with space returns accepted enrollment")
    void acceptNextWaitingEnrollment_fcfsWithSpace_returnsAccepted() {
        Event event = createFcfsEvent(2);
        Account waitingAccount = createAccount(1L);
        addEnrollment(event, waitingAccount, false);

        Enrollment result = event.acceptNextWaitingEnrollment();

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAccount()).isEqualTo(waitingAccount);
    }

    @Test
    @DisplayName("acceptNextWaitingEnrollment: no waiters returns null")
    void acceptNextWaitingEnrollment_noWaiters_returnsNull() {
        Event event = createFcfsEvent(2);

        Enrollment result = event.acceptNextWaitingEnrollment();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("acceptNextWaitingEnrollment: CONFIRMATIVE type returns null")
    void acceptNextWaitingEnrollment_confirmative_returnsNull() {
        Event event = createEvent(EventType.CONFIRMATIVE, 2);
        Account waitingAccount = createAccount(1L);
        addEnrollment(event, waitingAccount, false);

        Enrollment result = event.acceptNextWaitingEnrollment();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("acceptWaitingList: FCFS with 3 waiters and 2 spots returns 2 accepted")
    void acceptWaitingList_fcfsWithWaiters_returnsAccepted() {
        Event event = createFcfsEvent(2);
        Account account1 = createAccount(1L);
        Account account2 = createAccount(2L);
        Account account3 = createAccount(3L);
        addEnrollment(event, account1, false);
        addEnrollment(event, account2, false);
        addEnrollment(event, account3, false);

        List<Enrollment> result = event.acceptWaitingList();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Enrollment::isAccepted);
        assertThat(result.get(0).getAccount()).isEqualTo(account1);
        assertThat(result.get(1).getAccount()).isEqualTo(account2);
    }

    @Test
    @DisplayName("acceptWaitingList: no waiters returns empty list")
    void acceptWaitingList_noWaiters_returnsEmptyList() {
        Event event = createFcfsEvent(2);

        List<Enrollment> result = event.acceptWaitingList();

        assertThat(result).isEmpty();
    }

    private Event createFcfsEvent(int limit) {
        return createEvent(EventType.FCFS, limit);
    }

    private Event createEvent(EventType type, int limit) {
        return Event.builder()
                .id(1L)
                .title("Test Event")
                .eventType(type)
                .limitOfEnrollments(limit)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
    }

    private Account createAccount(Long seed) {
        return Account.builder()
                .id(UUID.randomUUID())
                .email("user" + seed + "@test.com")
                .nickname("user" + seed)
                .build();
    }

    private void addEnrollment(Event event, Account account, boolean accepted) {
        Enrollment enrollment = Enrollment.builder()
                .account(account)
                .enrolledAt(Instant.now())
                .accepted(accepted)
                .build();
        event.addEnrollment(enrollment);
    }
}
