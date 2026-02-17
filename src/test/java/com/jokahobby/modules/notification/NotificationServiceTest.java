package com.jokahobby.modules.notification;

import com.jokahobby.modules.account.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    private Account account;
    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .nickname("tester")
                .build();

        notification1 = Notification.builder()
                .id(1L)
                .title("Test Notification 1")
                .message("Message 1")
                .account(account)
                .notificationType(NotificationType.HOBBY_CREATED)
                .checked(false)
                .build();

        notification2 = Notification.builder()
                .id(2L)
                .title("Test Notification 2")
                .message("Message 2")
                .account(account)
                .notificationType(NotificationType.HOBBY_UPDATED)
                .checked(false)
                .build();
    }

    @Test
    @DisplayName("getNotifications returns notifications for account and checked status")
    void getNotifications_returnsNotifications() {
        given(notificationRepository.findByAccountAndCheckedOrderByCreatedAtDesc(account, false))
                .willReturn(List.of(notification1, notification2));

        List<Notification> result = notificationService.getNotifications(account, false);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(notification1, notification2);
    }

    @Test
    @DisplayName("getNotifications returns empty list when no notifications exist")
    void getNotifications_returnsEmptyList() {
        given(notificationRepository.findByAccountAndCheckedOrderByCreatedAtDesc(account, false))
                .willReturn(List.of());

        List<Notification> result = notificationService.getNotifications(account, false);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countNotifications returns count for account and checked status")
    void countNotifications_returnsCount() {
        given(notificationRepository.countByAccountAndChecked(account, false))
                .willReturn(5L);

        long result = notificationService.countNotifications(account, false);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("countNotifications returns zero when no notifications exist")
    void countNotifications_returnsZero() {
        given(notificationRepository.countByAccountAndChecked(account, true))
                .willReturn(0L);

        long result = notificationService.countNotifications(account, true);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("deleteReadNotifications delegates to repository with checked=true")
    void deleteReadNotifications_delegatesToRepository() {
        notificationService.deleteReadNotifications(account);

        verify(notificationRepository).deleteByAccountAndChecked(account, true);
    }

    @Test
    @DisplayName("markAsRead marks each notification and saves all")
    void markAsRead_marksAndSaves() {
        List<Notification> notifications = List.of(notification1, notification2);

        notificationService.markAsRead(notifications);

        assertThat(notification1.isChecked()).isTrue();
        assertThat(notification2.isChecked()).isTrue();
        verify(notificationRepository).saveAll(notifications);
    }

    @Test
    @DisplayName("NotificationService does not depend on ApplicationEventPublisher")
    void architecturalGuard_noEventPublisherDependency() {
        boolean hasEventPublisher = java.util.Arrays.stream(NotificationService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(ApplicationEventPublisher.class));

        assertThat(hasEventPublisher)
                .as("NotificationService should not depend on ApplicationEventPublisher")
                .isFalse();
    }
}
