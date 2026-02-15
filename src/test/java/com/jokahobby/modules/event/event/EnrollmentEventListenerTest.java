package com.jokahobby.modules.event.event;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.mail.EmailMessage;
import com.jokahobby.infra.mail.EmailService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.Enrollment;
import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.event.EventType;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentEventListenerTest {

    @InjectMocks
    private EnrollmentEventListener enrollmentEventListener;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppProperties appProperties;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailService emailService;

    private Account account;
    private Hobby hobby;
    private Event event;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        hobby = Hobby.builder()
                .id(1L)
                .path("test-hobby")
                .title("Test Hobby")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .hobby(hobby)
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(Duration.ofDays(7)))
                .startDateTime(Instant.now().plus(Duration.ofDays(8)))
                .endDateTime(Instant.now().plus(Duration.ofDays(9)))
                .build();
    }

    private Account buildAccount(boolean emailPref, boolean webPref) {
        return Account.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .nickname("tester")
                .hobbyEnrollmentResultByEmail(emailPref)
                .hobbyEnrollmentResultByWeb(webPref)
                .build();
    }

    private EnrollmentAcceptedEvent createAcceptedEvent(Account account) {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .account(account)
                .event(event)
                .enrolledAt(Instant.now())
                .accepted(true)
                .build();
        return new EnrollmentAcceptedEvent(enrollment);
    }

    @Nested
    @DisplayName("handleEnrollmentEvent")
    class HandleEnrollmentEvent {

        @Test
        @DisplayName("sends email when email preference is enabled")
        void sendsEmail_whenEmailPreferenceEnabled() {
            Account account = buildAccount(true, false);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);
            given(appProperties.getHost()).willReturn("http://localhost:8080");
            given(templateEngine.process(eq("mail/simple-link"), any(Context.class)))
                    .willReturn("<html>email</html>");

            enrollmentEventListener.handleEnrollmentEvent(event);

            ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
            verify(emailService).sendEmail(captor.capture());
            EmailMessage sent = captor.getValue();
            assertThat(sent.getTo()).isEqualTo("test@test.com");
            assertThat(sent.getSubject()).contains("Test Event");
        }

        @Test
        @DisplayName("does not send email when email preference is disabled")
        void doesNotSendEmail_whenEmailPreferenceDisabled() {
            Account account = buildAccount(false, false);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);

            enrollmentEventListener.handleEnrollmentEvent(event);

            verify(emailService, never()).sendEmail(any());
        }

        @Test
        @DisplayName("creates notification when web preference is enabled")
        void createsNotification_whenWebPreferenceEnabled() {
            Account account = buildAccount(false, true);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);

            enrollmentEventListener.handleEnrollmentEvent(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Test Hobby / Test Event");
            assertThat(saved.getLink()).isEqualTo("/hobby/test-hobby/events/1");
            assertThat(saved.getAccount()).isEqualTo(account);
            assertThat(saved.getNotificationType()).isEqualTo(NotificationType.EVENT_ENROLLMENT);
            assertThat(saved.isChecked()).isFalse();
        }

        @Test
        @DisplayName("does not create notification when web preference is disabled")
        void doesNotCreateNotification_whenWebPreferenceDisabled() {
            Account account = buildAccount(false, false);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);

            enrollmentEventListener.handleEnrollmentEvent(event);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("sends both email and notification when both preferences enabled")
        void sendsBoth_whenBothPreferencesEnabled() {
            Account account = buildAccount(true, true);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);
            given(appProperties.getHost()).willReturn("http://localhost:8080");
            given(templateEngine.process(eq("mail/simple-link"), any(Context.class)))
                    .willReturn("<html>email</html>");

            enrollmentEventListener.handleEnrollmentEvent(event);

            verify(emailService).sendEmail(any());
            verify(notificationRepository).save(any());
        }

        @Test
        @DisplayName("accepted event has correct message")
        void acceptedEvent_hasCorrectMessage() {
            Account account = buildAccount(false, true);
            EnrollmentAcceptedEvent event = createAcceptedEvent(account);

            enrollmentEventListener.handleEnrollmentEvent(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getMessage()).contains("confirmed");
        }

        @Test
        @DisplayName("rejected event has correct message")
        void rejectedEvent_hasCorrectMessage() {
            Account account = buildAccount(false, true);
            Enrollment enrollment = Enrollment.builder()
                    .id(1L).account(account).event(event)
                    .enrolledAt(Instant.now()).accepted(false).build();
            EnrollmentRejectedEvent rejectedEvent = new EnrollmentRejectedEvent(enrollment);

            enrollmentEventListener.handleEnrollmentEvent(rejectedEvent);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getMessage()).contains("rejected");
        }
    }
}
