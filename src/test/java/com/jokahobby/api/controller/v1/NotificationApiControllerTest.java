package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class NotificationApiControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JwtProvider jwtProvider;

    private Account testAccount;
    private Account otherAccount;
    private String accessToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(Account.builder()
                .email("test@example.com")
                .nickname("testuser")
                .provider("google")
                .providerId("google-123")
                .joinedAt(Instant.now())
                .build());
        accessToken = jwtProvider.createAccessToken(testAccount.getId());

        otherAccount = accountRepository.save(Account.builder()
                .email("other@example.com")
                .nickname("otheruser")
                .provider("google")
                .providerId("google-456")
                .joinedAt(Instant.now())
                .build());
        otherToken = jwtProvider.createAccessToken(otherAccount.getId());
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }

    private Notification createNotification(Account account, boolean checked, NotificationType type) {
        return notificationRepository.save(Notification.builder()
                .title("Test Notification")
                .link("/test-link")
                .message("Test message")
                .checked(checked)
                .account(account)
                .notificationType(type)
                .build());
    }

    // ===== GET /api/v1/notifications =====

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotifications {

        @Test
        @DisplayName("returns unchecked notifications by default")
        void returnsUncheckedByDefault() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, false, NotificationType.HOBBY_UPDATED);
            createNotification(testAccount, true, NotificationType.EVENT_ENROLLMENT);

            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(2);
        }

        @Test
        @DisplayName("returns unchecked notifications with checked=false")
        void returnsUncheckedExplicit() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, true, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.get().uri("/api/v1/notifications?checked=false")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(1);
        }

        @Test
        @DisplayName("returns checked notifications with checked=true")
        void returnsChecked() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, true, NotificationType.HOBBY_UPDATED);
            createNotification(testAccount, true, NotificationType.EVENT_ENROLLMENT);

            assertThat(mockMvc.get().uri("/api/v1/notifications?checked=true")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no notifications")
        void emptyList() {
            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().isEmpty();
        }

        @Test
        @DisplayName("includes counts in response")
        void includesCounts() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, true, NotificationType.EVENT_ENROLLMENT);

            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.counts.total").isEqualTo(3);

            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.counts.unchecked").isEqualTo(2);

            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.counts.checked").isEqualTo(1);
        }

        @Test
        @DisplayName("only returns authenticated user's notifications")
        void onlyOwnNotifications() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, false, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(1);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.get().uri("/api/v1/notifications"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== GET /api/v1/notifications/unread-count =====

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("returns correct unread count")
        void returnsCorrectCount() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, false, NotificationType.HOBBY_UPDATED);
            createNotification(testAccount, true, NotificationType.EVENT_ENROLLMENT);

            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.count").isEqualTo(2);
        }

        @Test
        @DisplayName("returns 0 when no unread notifications")
        void returnsZero() {
            createNotification(testAccount, true, NotificationType.HOBBY_CREATED);

            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.count").isEqualTo(0);
        }

        @Test
        @DisplayName("only counts authenticated user's notifications")
        void onlyOwnNotifications() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, false, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.count").isEqualTo(1);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== PATCH /api/v1/notifications/mark-as-read =====

    @Nested
    @DisplayName("PATCH /api/v1/notifications/mark-as-read")
    class MarkAsRead {

        @Test
        @DisplayName("marks all unread notifications as read")
        void marksAllAsRead() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, false, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.patch().uri("/api/v1/notifications/mark-as-read")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            // Verify all are now read
            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.count").isEqualTo(0);
        }

        @Test
        @DisplayName("succeeds when all already read (idempotent)")
        void alreadyAllRead() {
            createNotification(testAccount, true, NotificationType.HOBBY_CREATED);

            assertThat(mockMvc.patch().uri("/api/v1/notifications/mark-as-read")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("only marks authenticated user's notifications")
        void onlyOwnNotifications() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, false, NotificationType.HOBBY_CREATED);

            assertThat(mockMvc.patch().uri("/api/v1/notifications/mark-as-read")
                            .header("Authorization", bearer()))
                    .hasStatusOk();

            // Other user's notification should still be unread
            assertThat(mockMvc.get().uri("/api/v1/notifications/unread-count")
                            .header("Authorization", "Bearer " + otherToken))
                    .bodyJson()
                    .extractingPath("$.data.count").isEqualTo(1);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.patch().uri("/api/v1/notifications/mark-as-read"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== DELETE /api/v1/notifications =====

    @Nested
    @DisplayName("DELETE /api/v1/notifications")
    class DeleteReadNotifications {

        @Test
        @DisplayName("deletes all read notifications")
        void deletesReadNotifications() {
            createNotification(testAccount, true, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, true, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.delete().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            // Verify read notifications are gone
            assertThat(mockMvc.get().uri("/api/v1/notifications?checked=true")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().isEmpty();
        }

        @Test
        @DisplayName("preserves unread notifications")
        void preservesUnreadNotifications() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);
            createNotification(testAccount, true, NotificationType.HOBBY_UPDATED);

            assertThat(mockMvc.delete().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk();

            // Unread notification should still exist
            assertThat(mockMvc.get().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(1);
        }

        @Test
        @DisplayName("succeeds when no read notifications (idempotent)")
        void noReadNotifications() {
            createNotification(testAccount, false, NotificationType.HOBBY_CREATED);

            assertThat(mockMvc.delete().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("only deletes authenticated user's notifications")
        void onlyOwnNotifications() {
            createNotification(testAccount, true, NotificationType.HOBBY_CREATED);
            createNotification(otherAccount, true, NotificationType.HOBBY_CREATED);

            assertThat(mockMvc.delete().uri("/api/v1/notifications")
                            .header("Authorization", bearer()))
                    .hasStatusOk();

            // Other user's read notification should still exist
            assertThat(mockMvc.get().uri("/api/v1/notifications?checked=true")
                            .header("Authorization", "Bearer " + otherToken))
                    .bodyJson()
                    .extractingPath("$.data.notifications").asArray().hasSize(1);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.delete().uri("/api/v1/notifications"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
