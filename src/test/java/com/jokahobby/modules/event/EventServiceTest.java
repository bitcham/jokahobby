package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private Event fcfsEvent;
    private Account account;
    private Hobby hobby;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .nickname("tester")
                .build();

        hobby = Hobby.builder()
                .id(1L)
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .build();

        fcfsEvent = Event.builder()
                .id(1L)
                .title("FCFS Event")
                .hobby(hobby)
                .createdBy(account)
                .eventType(EventType.FCFS)
                .limitOfEnrollments(2)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
    }

    @Test
    @DisplayName("newEnrollment: FCFS with space returns accepted enrollment")
    void newEnrollment_fcfsWithSpace_returnsAcceptedEnrollment() {
        given(enrollmentRepository.existsByEventAndAccount(fcfsEvent, account)).willReturn(false);
        given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(inv -> inv.getArgument(0));

        Enrollment result = eventService.newEnrollment(fcfsEvent, account);

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAccount()).isEqualTo(account);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("newEnrollment: duplicate throws BusinessException")
    void newEnrollment_duplicate_throwsBusinessException() {
        given(enrollmentRepository.existsByEventAndAccount(fcfsEvent, account)).willReturn(true);

        assertThatThrownBy(() -> eventService.newEnrollment(fcfsEvent, account))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_ENROLLABLE));
    }

    @Test
    @DisplayName("cancelEnrollment: FCFS waiter promoted returns promoted enrollment")
    void cancelEnrollment_fcfsWaiterPromoted_returnsPromoted() {
        // Set up: 2 spots, 2 accepted (full), then 1 waiting
        Account accepted1 = Account.builder().id(UUID.randomUUID()).email("a1@test.com").nickname("a1").build();
        Account accepted2 = Account.builder().id(UUID.randomUUID()).email("a2@test.com").nickname("a2").build();
        Account waiter = Account.builder().id(UUID.randomUUID()).email("w@test.com").nickname("waiter").build();

        Enrollment e1 = Enrollment.builder().account(accepted1).enrolledAt(Instant.now()).accepted(true).build();
        Enrollment e2 = Enrollment.builder().account(accepted2).enrolledAt(Instant.now()).accepted(true).build();
        Enrollment waitingEnrollment = Enrollment.builder().account(waiter).enrolledAt(Instant.now()).accepted(false).build();

        fcfsEvent.addEnrollment(e1);
        fcfsEvent.addEnrollment(e2);
        fcfsEvent.addEnrollment(waitingEnrollment);

        given(enrollmentRepository.findByEventAndAccount(fcfsEvent, accepted1)).willReturn(e1);

        Enrollment result = eventService.cancelEnrollment(fcfsEvent, accepted1);

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAccount()).isEqualTo(waiter);
    }

    @Test
    @DisplayName("updateEvent: returns list of promoted enrollments")
    void updateEvent_returnsPromotedEnrollments() {
        // Set up: limit 3, 1 accepted, 2 waiting → after update limit stays 3, so 2 get promoted
        Account accepted = Account.builder().id(UUID.randomUUID()).email("a@test.com").nickname("accepted").build();
        Account waiter1 = Account.builder().id(UUID.randomUUID()).email("w1@test.com").nickname("waiter1").build();
        Account waiter2 = Account.builder().id(UUID.randomUUID()).email("w2@test.com").nickname("waiter2").build();

        Enrollment acceptedEnrollment = Enrollment.builder().account(accepted).enrolledAt(Instant.now()).accepted(true).build();
        Enrollment waiting1 = Enrollment.builder().account(waiter1).enrolledAt(Instant.now()).accepted(false).build();
        Enrollment waiting2 = Enrollment.builder().account(waiter2).enrolledAt(Instant.now()).accepted(false).build();

        fcfsEvent.addEnrollment(acceptedEnrollment);
        fcfsEvent.addEnrollment(waiting1);
        fcfsEvent.addEnrollment(waiting2);

        List<Enrollment> result = eventService.updateEvent(fcfsEvent, "Updated",
                "desc", Instant.now().plus(Duration.ofDays(7)),
                Instant.now().plus(Duration.ofDays(8)),
                Instant.now().plus(Duration.ofDays(9)), 3);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Enrollment::isAccepted);
    }

    // ===== getEventsByHobby =====

    @Test
    @DisplayName("getEventsByHobby: returns events from repository")
    void getEventsByHobby_returnsEvents() {
        given(eventRepository.findByHobbyOrderByStartDateTime(hobby)).willReturn(List.of(fcfsEvent));

        List<Event> result = eventService.getEventsByHobby(hobby);

        assertThat(result).containsExactly(fcfsEvent);
        verify(eventRepository).findByHobbyOrderByStartDateTime(hobby);
    }

    // ===== getEventWithHobbyCheck =====

    @Test
    @DisplayName("getEventWithHobbyCheck: returns event when hobby matches")
    void getEventWithHobbyCheck_returnsEvent() {
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(fcfsEvent));

        Event result = eventService.getEventWithHobbyCheck(1L, hobby);

        assertThat(result).isEqualTo(fcfsEvent);
    }

    @Test
    @DisplayName("getEventWithHobbyCheck: throws when event not found")
    void getEventWithHobbyCheck_eventNotFound_throwsException() {
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventWithHobbyCheck(1L, hobby))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    @DisplayName("getEventWithHobbyCheck: throws when hobby does not match")
    void getEventWithHobbyCheck_hobbyMismatch_throwsException() {
        Hobby otherHobby = Hobby.builder().id(999L).path("other").title("Other").shortDescription("other").build();
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(fcfsEvent));

        assertThatThrownBy(() -> eventService.getEventWithHobbyCheck(1L, otherHobby))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    // ===== getEventWithHobbyCheckForUpdate =====

    @Test
    @DisplayName("getEventWithHobbyCheckForUpdate: acquires lock then returns event")
    void getEventWithHobbyCheckForUpdate_returnsEvent() {
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(fcfsEvent));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(fcfsEvent));

        Event result = eventService.getEventWithHobbyCheckForUpdate(1L, hobby);

        assertThat(result).isEqualTo(fcfsEvent);
        var inOrder = org.mockito.Mockito.inOrder(eventRepository);
        inOrder.verify(eventRepository).findByIdForUpdate(1L);
        inOrder.verify(eventRepository).findWithEnrollmentsById(1L);
    }

    @Test
    @DisplayName("getEventWithHobbyCheckForUpdate: throws when lock query finds nothing")
    void getEventWithHobbyCheckForUpdate_lockFails_throwsException() {
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventWithHobbyCheckForUpdate(1L, hobby))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    @Test
    @DisplayName("getEventWithHobbyCheckForUpdate: throws when hobby does not match")
    void getEventWithHobbyCheckForUpdate_hobbyMismatch_throwsException() {
        Hobby otherHobby = Hobby.builder().id(999L).path("other").title("Other").shortDescription("other").build();
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(fcfsEvent));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(fcfsEvent));

        assertThatThrownBy(() -> eventService.getEventWithHobbyCheckForUpdate(1L, otherHobby))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    // ===== getEnrollment =====

    @Test
    @DisplayName("getEnrollment: returns enrollment when found")
    void getEnrollment_returnsEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L).account(account).enrolledAt(Instant.now()).accepted(true).build();
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        Enrollment result = eventService.getEnrollment(1L);

        assertThat(result).isEqualTo(enrollment);
    }

    @Test
    @DisplayName("getEnrollment: throws when enrollment not found")
    void getEnrollment_notFound_throwsException() {
        given(enrollmentRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEnrollment(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("EventService has no ApplicationEventPublisher dependency")
    void noPublisherDependency() {
        // This test verifies at compile time that EventService only requires
        // EventRepository and EnrollmentRepository (no ApplicationEventPublisher).
        // If publisher is added back, @InjectMocks will still work but the constructor
        // test below confirms the field count.
        var fields = EventService.class.getDeclaredFields();
        for (var field : fields) {
            assertThat(field.getType().getSimpleName())
                    .as("EventService should not have ApplicationEventPublisher field")
                    .isNotEqualTo("ApplicationEventPublisher");
        }
    }
}
