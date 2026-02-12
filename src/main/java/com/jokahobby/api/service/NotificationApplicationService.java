package com.jokahobby.api.service;

import com.jokahobby.api.dto.response.*;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationService;
import com.jokahobby.modules.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public NotificationListResponse getNotifications(Account account, boolean checked) {
        List<Notification> notifications = notificationRepository
                .findByAccountAndCheckedOrderByCreatedAtDesc(account, checked);

        long uncheckedCount = notificationRepository.countByAccountAndChecked(account, false);
        long checkedCount = notificationRepository.countByAccountAndChecked(account, true);

        Map<NotificationType, Long> byType = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getNotificationType, Collectors.counting()));

        List<NotificationResponse> items = notifications.stream()
                .map(NotificationResponse::from)
                .toList();

        NotificationCountResponse counts = new NotificationCountResponse(
                uncheckedCount + checkedCount, uncheckedCount, checkedCount, byType);

        return new NotificationListResponse(items, counts);
    }

    public UnreadCountResponse getUnreadCount(Account account) {
        long count = notificationRepository.countByAccountAndChecked(account, false);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAsRead(Account account) {
        List<Notification> unchecked = notificationRepository
                .findByAccountAndCheckedOrderByCreatedAtDesc(account, false);
        notificationService.markAsRead(unchecked);
    }

    @Transactional
    public void deleteReadNotifications(Account account) {
        notificationRepository.deleteByAccountAndChecked(account, true);
    }
}
