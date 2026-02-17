package com.jokahobby.modules.notification;

import com.jokahobby.modules.account.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotifications(Account account, boolean checked) {
        return notificationRepository.findByAccountAndCheckedOrderByCreatedAtDesc(account, checked);
    }

    public long countNotifications(Account account, boolean checked) {
        return notificationRepository.countByAccountAndChecked(account, checked);
    }

    public void deleteReadNotifications(Account account) {
        notificationRepository.deleteByAccountAndChecked(account, true);
    }

    public void markAsRead(List<Notification> notifications) {
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }
}
