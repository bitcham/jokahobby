package com.jokahobby.modules.event;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // ===== P0: limitOfEnrollments NPE defense (unlimited events) =====

    @Test
    @DisplayName("numberOfRemainSpots: unlimited event returns Integer.MAX_VALUE")
    void numberOfRemainSpots_unlimitedEvent_returnsMaxValue() {
        Event event = createUnlimitedFcfsEvent();
        addEnrollment(event, createAccount(1L), true);

        int result = event.numberOfRemainSpots();

        assertThat(result).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("isAbleToAcceptWaitingEnrollment: unlimited FCFS returns true")
    void isAbleToAcceptWaitingEnrollment_unlimitedFcfs_returnsTrue() {
        Event event = createUnlimitedFcfsEvent();
        addEnrollment(event, createAccount(1L), true);
        addEnrollment(event, createAccount(2L), true);

        boolean result = event.isAbleToAcceptWaitingEnrollment();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("acceptNextWaitingEnrollment: unlimited FCFS accepts waiter")
    void acceptNextWaitingEnrollment_unlimitedFcfs_acceptsWaiter() {
        Event event = createUnlimitedFcfsEvent();
        Account waiter = createAccount(1L);
        addEnrollment(event, waiter, false);

        Enrollment result = event.acceptNextWaitingEnrollment();

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    @DisplayName("acceptWaitingList: unlimited FCFS accepts all waiters")
    void acceptWaitingList_unlimitedFcfs_acceptsAll() {
        Event event = createUnlimitedFcfsEvent();
        addEnrollment(event, createAccount(1L), false);
        addEnrollment(event, createAccount(2L), false);
        addEnrollment(event, createAccount(3L), false);

        List<Enrollment> result = event.acceptWaitingList();

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(Enrollment::isAccepted);
    }

    @Test
    @DisplayName("accept: unlimited CONFIRMATIVE accepts enrollment")
    void accept_unlimitedConfirmative_acceptsEnrollment() {
        Event event = createUnlimitedEvent(EventType.CONFIRMATIVE);
        Account account = createAccount(1L);
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(false).build();
        event.addEnrollment(enrollment);

        event.accept(enrollment);

        assertThat(enrollment.isAccepted()).isTrue();
    }

    // ===== P1-2: Event.accept()/reject() silent failure =====

    @Test
    @DisplayName("accept: non-CONFIRMATIVE type throws BusinessException")
    void accept_nonConfirmative_throwsException() {
        Event event = createFcfsEvent(10);
        Account account = createAccount(1L);
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(false).build();
        event.addEnrollment(enrollment);

        assertThatThrownBy(() -> event.accept(enrollment))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_CANNOT_ACCEPT));
    }

    @Test
    @DisplayName("accept: over limit throws BusinessException")
    void accept_overLimit_throwsException() {
        Event event = createEvent(EventType.CONFIRMATIVE, 1);
        addEnrollment(event, createAccount(1L), true); // fills the 1 spot
        Account account = createAccount(2L);
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(false).build();
        event.addEnrollment(enrollment);

        assertThatThrownBy(() -> event.accept(enrollment))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_CANNOT_ACCEPT));
    }

    @Test
    @DisplayName("reject: non-CONFIRMATIVE type throws BusinessException")
    void reject_nonConfirmative_throwsException() {
        Event event = createFcfsEvent(10);
        Account account = createAccount(1L);
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(true).build();
        event.addEnrollment(enrollment);

        assertThatThrownBy(() -> event.reject(enrollment))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_CANNOT_REJECT));
    }

    @Test
    @DisplayName("reject: CONFIRMATIVE type rejects enrollment")
    void reject_confirmative_rejectsEnrollment() {
        Event event = createEvent(EventType.CONFIRMATIVE, 2);
        Account account = createAccount(1L);
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(true).build();
        event.addEnrollment(enrollment);

        event.reject(enrollment);

        assertThat(enrollment.isAccepted()).isFalse();
    }

    // ===== Helper methods =====

    private Event createUnlimitedFcfsEvent() {
        return createUnlimitedEvent(EventType.FCFS);
    }

    private Event createUnlimitedEvent(EventType type) {
        return Event.builder()
                .id(1L)
                .title("Unlimited Event")
                .eventType(type)
                .limitOfEnrollments(null)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
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
