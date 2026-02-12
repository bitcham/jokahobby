package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.NotificationListResponse;
import com.jokahobby.api.dto.response.UnreadCountResponse;
import com.jokahobby.api.service.NotificationApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationApplicationService notificationApplicationService;

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @CurrentAccount Account account,
            @RequestParam(defaultValue = "false") boolean checked) {
        NotificationListResponse response = notificationApplicationService.getNotifications(account, checked);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @CurrentAccount Account account) {
        UnreadCountResponse response = notificationApplicationService.getUnreadCount(account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/api/v1/notifications/mark-as-read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @CurrentAccount Account account) {
        notificationApplicationService.markAsRead(account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/notifications")
    public ResponseEntity<ApiResponse<Void>> deleteReadNotifications(
            @CurrentAccount Account account) {
        notificationApplicationService.deleteReadNotifications(account);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
