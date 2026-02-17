package com.jokahobby.api.service;

import com.jokahobby.api.dto.response.NotificationListResponse;
import com.jokahobby.api.dto.response.UnreadCountResponse;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationService;
import com.jokahobby.modules.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @InjectMocks
    private NotificationApplicationService notificationApplicationService;

    @Mock
    private NotificationService notificationService;

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
                .title("Notification 1")
                .message("Message 1")
                .link("/link1")
                .account(account)
                .notificationType(NotificationType.HOBBY_CREATED)
                .checked(false)
                .build();

        notification2 = Notification.builder()
                .id(2L)
                .title("Notification 2")
                .message("Message 2")
                .link("/link2")
                .account(account)
                .notificationType(NotificationType.HOBBY_UPDATED)
                .checked(false)
                .build();
    }

    @Test
    @DisplayName("getNotifications returns notification list with counts")
    void getNotifications_returnsListWithCounts() {
        given(notificationService.getNotifications(account, false))
                .willReturn(List.of(notification1, notification2));
        given(notificationService.countNotifications(account, false)).willReturn(2L);
        given(notificationService.countNotifications(account, true)).willReturn(3L);

        NotificationListResponse response = notificationApplicationService.getNotifications(account, false);

        assertThat(response.notifications()).hasSize(2);
        assertThat(response.counts().unchecked()).isEqualTo(2L);
        assertThat(response.counts().checked()).isEqualTo(3L);
        assertThat(response.counts().total()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getNotifications returns empty response when no notifications")
    void getNotifications_returnsEmptyResponse() {
        given(notificationService.getNotifications(account, false)).willReturn(List.of());
        given(notificationService.countNotifications(account, false)).willReturn(0L);
        given(notificationService.countNotifications(account, true)).willReturn(0L);

        NotificationListResponse response = notificationApplicationService.getNotifications(account, false);

        assertThat(response.notifications()).isEmpty();
        assertThat(response.counts().total()).isZero();
    }

    @Test
    @DisplayName("getUnreadCount delegates to notification service")
    void getUnreadCount_delegatesToService() {
        given(notificationService.countNotifications(account, false)).willReturn(7L);

        UnreadCountResponse response = notificationApplicationService.getUnreadCount(account);

        assertThat(response.count()).isEqualTo(7L);
    }

    @Test
    @DisplayName("markAsRead fetches unchecked notifications and delegates to service")
    void markAsRead_delegatesToService() {
        List<Notification> unchecked = List.of(notification1, notification2);
        given(notificationService.getNotifications(account, false)).willReturn(unchecked);

        notificationApplicationService.markAsRead(account);

        verify(notificationService).markAsRead(unchecked);
    }

    @Test
    @DisplayName("markAsRead calls markAsRead with empty list when no unchecked notifications")
    void markAsRead_emptyList() {
        given(notificationService.getNotifications(account, false)).willReturn(List.of());

        notificationApplicationService.markAsRead(account);

        verify(notificationService).markAsRead(List.of());
    }

    @Test
    @DisplayName("deleteReadNotifications delegates to notification service")
    void deleteReadNotifications_delegatesToService() {
        notificationApplicationService.deleteReadNotifications(account);

        verify(notificationService).deleteReadNotifications(account);
    }

    @Test
    @DisplayName("NotificationApplicationService does not depend on NotificationRepository")
    void architecturalGuard_noRepositoryDependency() {
        boolean hasRepository = java.util.Arrays.stream(NotificationApplicationService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(NotificationRepository.class));

        assertThat(hasRepository)
                .as("NotificationApplicationService should not depend on NotificationRepository directly")
                .isFalse();
    }
}
