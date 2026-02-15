package com.jokahobby.api.service;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.*;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventApplicationServiceTest {

    @InjectMocks
    private EventApplicationService eventApplicationService;

    @Mock
    private HobbyService hobbyService;

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Hobby hobby;
    private Account manager;
    private Account enrollee;

    @BeforeEach
    void setUp() {
        manager = Account.builder()
                .id(UUID.randomUUID())
                .email("manager@test.com")
                .nickname("manager")
                .build();

        enrollee = Account.builder()
                .id(UUID.randomUUID())
                .email("enrollee@test.com")
                .nickname("enrollee")
                .build();

        hobby = Hobby.builder()
                .id(1L)
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .published(true)
                .recruiting(true)
                .build();
    }

    @Test
    @DisplayName("enroll FCFS auto-accepted publishes EnrollmentAcceptedEvent")
    void enroll_fcfsAutoAccepted_publishesEvent() {
        Event event = createFcfsEvent();
        Enrollment acceptedEnrollment = Enrollment.builder()
                .id(1L).account(enrollee).enrolledAt(Instant.now()).accepted(true).build();

        given(hobbyService.getHobby("test-hobby")).willReturn(hobby);
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(event));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(event));
        given(eventService.newEnrollment(event, enrollee)).willReturn(acceptedEnrollment);

        eventApplicationService.enroll("test-hobby", 1L, enrollee);

        verify(eventPublisher).publishEvent(any(EnrollmentAcceptedEvent.class));
    }

    @Test
    @DisplayName("enroll CONFIRMATIVE does not publish event")
    void enroll_confirmative_noEvent() {
        Event event = createConfirmativeEvent();
        Enrollment waitingEnrollment = Enrollment.builder()
                .id(1L).account(enrollee).enrolledAt(Instant.now()).accepted(false).build();

        given(hobbyService.getHobby("test-hobby")).willReturn(hobby);
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(event));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(event));
        given(eventService.newEnrollment(event, enrollee)).willReturn(waitingEnrollment);

        eventApplicationService.enroll("test-hobby", 1L, enrollee);

        verify(eventPublisher, never()).publishEvent(any(EnrollmentAcceptedEvent.class));
    }

    @Test
    @DisplayName("disenroll FCFS waiter promoted publishes EnrollmentAcceptedEvent")
    void disenroll_fcfsWaiterPromoted_publishesEvent() {
        Event event = createFcfsEvent();
        addEnrollmentToEvent(event, enrollee, true);
        Enrollment promotedEnrollment = Enrollment.builder()
                .id(2L).account(enrollee).enrolledAt(Instant.now()).accepted(true).build();

        given(hobbyService.getHobby("test-hobby")).willReturn(hobby);
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(event));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(event));
        given(eventService.cancelEnrollment(event, enrollee)).willReturn(promotedEnrollment);

        eventApplicationService.disenroll("test-hobby", 1L, enrollee);

        verify(eventPublisher).publishEvent(any(EnrollmentAcceptedEvent.class));
    }

    @Test
    @DisplayName("disenroll no waiter promoted does not publish event")
    void disenroll_noWaiterPromoted_noEvent() {
        Event event = createFcfsEvent();
        addEnrollmentToEvent(event, enrollee, true);

        given(hobbyService.getHobby("test-hobby")).willReturn(hobby);
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(event));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(event));
        given(eventService.cancelEnrollment(event, enrollee)).willReturn(null);

        eventApplicationService.disenroll("test-hobby", 1L, enrollee);

        verify(eventPublisher, never()).publishEvent(any(EnrollmentAcceptedEvent.class));
    }

    @Test
    @DisplayName("updateEvent batch promotion publishes EnrollmentAcceptedEvent for each")
    void updateEvent_batchPromotion_publishesEvents() {
        Event event = createFcfsEvent();
        Enrollment promoted1 = Enrollment.builder()
                .id(1L).account(enrollee).enrolledAt(Instant.now()).accepted(true).build();
        Account enrollee2 = Account.builder().id(UUID.randomUUID()).email("e2@test.com").nickname("e2").build();
        Enrollment promoted2 = Enrollment.builder()
                .id(2L).account(enrollee2).enrolledAt(Instant.now()).accepted(true).build();

        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);
        given(eventRepository.findByIdForUpdate(1L)).willReturn(Optional.of(event));
        given(eventRepository.findWithEnrollmentsById(1L)).willReturn(Optional.of(event));
        given(eventService.updateEvent(eq(event), any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(promoted1, promoted2));

        eventApplicationService.updateEvent("test-hobby", 1L, manager,
                new com.jokahobby.api.dto.request.EventUpdateRequest(
                        "Updated", "desc",
                        Instant.now().plus(Duration.ofDays(7)),
                        Instant.now().plus(Duration.ofDays(8)),
                        Instant.now().plus(Duration.ofDays(9)), 5));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
        long acceptedEventCount = captor.getAllValues().stream()
                .filter(e -> e instanceof EnrollmentAcceptedEvent).count();
        assertThat(acceptedEventCount).isEqualTo(2);
    }

    @Test
    @DisplayName("createEvent publishes HobbyUpdateEvent")
    void createEvent_publishesHobbyUpdateEvent() {
        Event event = createFcfsEvent();

        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);
        given(eventService.createEvent(any(Event.class))).willReturn(event);

        eventApplicationService.createEvent("test-hobby", manager,
                new com.jokahobby.api.dto.request.EventCreateRequest(
                        "New Event", "desc", EventType.FCFS,
                        Instant.now().plus(Duration.ofDays(7)),
                        Instant.now().plus(Duration.ofDays(8)),
                        Instant.now().plus(Duration.ofDays(9)), 10));

        verify(eventPublisher).publishEvent(any(HobbyUpdateEvent.class));
    }

    @Test
    @DisplayName("acceptEnrollment publishes EnrollmentAcceptedEvent")
    void acceptEnrollment_publishesEvent() {
        Event event = createConfirmativeEvent();
        Enrollment enrollment = Enrollment.builder()
                .id(1L).account(enrollee).enrolledAt(Instant.now()).accepted(false).build();
        event.addEnrollment(enrollment);

        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        eventApplicationService.acceptEnrollment("test-hobby", 1L, manager);

        verify(eventPublisher).publishEvent(any(EnrollmentAcceptedEvent.class));
    }

    @Test
    @DisplayName("rejectEnrollment publishes EnrollmentRejectedEvent")
    void rejectEnrollment_publishesEvent() {
        Event event = createConfirmativeEvent();
        Enrollment enrollment = Enrollment.builder()
                .id(1L).account(enrollee).enrolledAt(Instant.now()).accepted(true).build();
        event.addEnrollment(enrollment);

        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);
        given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

        eventApplicationService.rejectEnrollment("test-hobby", 1L, manager);

        verify(eventPublisher).publishEvent(any(EnrollmentRejectedEvent.class));
    }

    private Event createFcfsEvent() {
        return Event.builder()
                .id(1L)
                .title("FCFS Event")
                .hobby(hobby)
                .createdBy(manager)
                .eventType(EventType.FCFS)
                .limitOfEnrollments(10)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
    }

    private void addEnrollmentToEvent(Event event, Account account, boolean accepted) {
        Enrollment enrollment = Enrollment.builder()
                .account(account).enrolledAt(Instant.now()).accepted(accepted).build();
        event.addEnrollment(enrollment);
    }

    private Event createConfirmativeEvent() {
        return Event.builder()
                .id(1L)
                .title("Confirmative Event")
                .hobby(hobby)
                .createdBy(manager)
                .eventType(EventType.CONFIRMATIVE)
                .limitOfEnrollments(10)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
    }
}
