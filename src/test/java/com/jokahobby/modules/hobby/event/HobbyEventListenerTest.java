package com.jokahobby.modules.hobby.event;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.mail.EmailMessage;
import com.jokahobby.infra.mail.EmailService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.*;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationType;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HobbyEventListenerTest {

    @InjectMocks
    private HobbyEventListener hobbyEventListener;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private HobbyTagRepository hobbyTagRepository;

    @Mock
    private HobbyZoneRepository hobbyZoneRepository;

    @Mock
    private HobbyManagerRepository hobbyManagerRepository;

    @Mock
    private HobbyMemberRepository hobbyMemberRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private AppProperties appProperties;

    @Mock
    private NotificationRepository notificationRepository;

    private Hobby hobby;
    private Tag tag;
    private Zone zone;

    @BeforeEach
    void setUp() {
        hobby = Hobby.builder()
                .id(1L)
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("A test hobby")
                .published(true)
                .build();

        tag = Tag.builder().id(1L).title("spring").build();
        zone = Zone.builder().id(1L).country("KR").city("Seoul").localNameOfCity("서울").province("none").build();
    }

    private Account buildAccount(boolean emailPref, boolean webPref) {
        return Account.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .nickname("user")
                .hobbyCreatedByEmail(emailPref)
                .hobbyCreatedByWeb(webPref)
                .hobbyUpdatedByEmail(emailPref)
                .hobbyUpdatedByWeb(webPref)
                .build();
    }

    @Nested
    @DisplayName("handleHobbyCreatedEvent")
    class HandleHobbyCreatedEvent {

        private void stubHobbyWithTagsAndZones(List<Account> matchedAccounts) {
            given(hobbyRepository.findById(1L)).willReturn(Optional.of(hobby));
            given(hobbyTagRepository.findAllByHobbyId(1L))
                    .willReturn(List.of(HobbyTag.builder().hobby(hobby).tag(tag).build()));
            given(hobbyZoneRepository.findAllByHobbyId(1L))
                    .willReturn(List.of(HobbyZone.builder().hobby(hobby).zone(zone).build()));
            given(accountRepository.findAll(any(com.querydsl.core.types.Predicate.class)))
                    .willReturn(matchedAccounts);
        }

        @Test
        @DisplayName("sends email when account has email preference enabled")
        void sendsEmail_whenEmailPreferenceEnabled() {
            Account account = buildAccount(true, false);
            stubHobbyWithTagsAndZones(List.of(account));
            given(appProperties.getHost()).willReturn("http://localhost:8080");
            given(templateEngine.process(eq("mail/simple-link"), any(Context.class)))
                    .willReturn("<html>email</html>");

            hobbyEventListener.handleHobbyCreatedEvent(new HobbyCreatedEvent(hobby));

            ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
            verify(emailService).sendEmail(captor.capture());
            assertThat(captor.getValue().getTo()).isEqualTo("user@test.com");
            assertThat(captor.getValue().getSubject()).contains("Test Hobby");
        }

        @Test
        @DisplayName("creates notification when account has web preference enabled")
        void createsNotification_whenWebPreferenceEnabled() {
            Account account = buildAccount(false, true);
            stubHobbyWithTagsAndZones(List.of(account));

            hobbyEventListener.handleHobbyCreatedEvent(new HobbyCreatedEvent(hobby));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Test Hobby");
            assertThat(saved.getLink()).isEqualTo("/hobby/test-hobby");
            assertThat(saved.getMessage()).isEqualTo("A test hobby");
            assertThat(saved.getAccount()).isEqualTo(account);
            assertThat(saved.getNotificationType()).isEqualTo(NotificationType.HOBBY_CREATED);
            assertThat(saved.isChecked()).isFalse();
        }

        @Test
        @DisplayName("does nothing when account has all preferences disabled")
        void doesNothing_whenAllPreferencesDisabled() {
            Account account = buildAccount(false, false);
            stubHobbyWithTagsAndZones(List.of(account));

            hobbyEventListener.handleHobbyCreatedEvent(new HobbyCreatedEvent(hobby));

            verify(emailService, never()).sendEmail(any());
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notifies multiple matching accounts")
        void notifiesMultipleAccounts() {
            Account account1 = buildAccount(false, true);
            Account account2 = Account.builder()
                    .id(UUID.randomUUID()).email("user2@test.com").nickname("user2")
                    .hobbyCreatedByWeb(true).build();
            stubHobbyWithTagsAndZones(List.of(account1, account2));

            hobbyEventListener.handleHobbyCreatedEvent(new HobbyCreatedEvent(hobby));

            verify(notificationRepository, times(2)).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("handleHobbyUpdateEvent")
    class HandleHobbyUpdateEvent {

        private void stubHobbyWithManagersAndMembers(Account manager, Account member) {
            given(hobbyRepository.findById(1L)).willReturn(Optional.of(hobby));
            given(hobbyManagerRepository.findAllByHobbyId(1L))
                    .willReturn(List.of(HobbyManager.builder().hobby(hobby).account(manager).build()));
            given(hobbyMemberRepository.findAllByHobbyId(1L))
                    .willReturn(List.of(HobbyMember.builder().hobby(hobby).account(member).build()));
        }

        @Test
        @DisplayName("sends email to managers and members with email preference")
        void sendsEmail_toManagersAndMembers() {
            Account manager = buildAccount(true, false);
            Account member = Account.builder()
                    .id(UUID.randomUUID()).email("member@test.com").nickname("member")
                    .hobbyUpdatedByEmail(true).build();
            stubHobbyWithManagersAndMembers(manager, member);
            given(appProperties.getHost()).willReturn("http://localhost:8080");
            given(templateEngine.process(eq("mail/simple-link"), any(Context.class)))
                    .willReturn("<html>email</html>");

            hobbyEventListener.handleHobbyUpdateEvent(new HobbyUpdateEvent(hobby, "New event added"));

            verify(emailService, times(2)).sendEmail(any(EmailMessage.class));
        }

        @Test
        @DisplayName("creates notifications for managers and members with web preference")
        void createsNotifications_forManagersAndMembers() {
            Account manager = buildAccount(false, true);
            Account member = Account.builder()
                    .id(UUID.randomUUID()).email("member@test.com").nickname("member")
                    .hobbyUpdatedByWeb(true).build();
            stubHobbyWithManagersAndMembers(manager, member);

            hobbyEventListener.handleHobbyUpdateEvent(new HobbyUpdateEvent(hobby, "New event added"));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(2)).save(captor.capture());
            captor.getAllValues().forEach(n -> {
                assertThat(n.getNotificationType()).isEqualTo(NotificationType.HOBBY_UPDATED);
                assertThat(n.getMessage()).isEqualTo("New event added");
            });
        }

        @Test
        @DisplayName("deduplicates account that is both manager and member")
        void deduplicatesAccount_whenBothManagerAndMember() {
            Account sameAccount = buildAccount(false, true);
            stubHobbyWithManagersAndMembers(sameAccount, sameAccount);

            hobbyEventListener.handleHobbyUpdateEvent(new HobbyUpdateEvent(hobby, "Updated"));

            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("does nothing when all preferences disabled")
        void doesNothing_whenAllPreferencesDisabled() {
            Account manager = buildAccount(false, false);
            Account member = Account.builder()
                    .id(UUID.randomUUID()).email("member@test.com").nickname("member").build();
            stubHobbyWithManagersAndMembers(manager, member);

            hobbyEventListener.handleHobbyUpdateEvent(new HobbyUpdateEvent(hobby, "Updated"));

            verify(emailService, never()).sendEmail(any());
            verify(notificationRepository, never()).save(any());
        }
    }
}
