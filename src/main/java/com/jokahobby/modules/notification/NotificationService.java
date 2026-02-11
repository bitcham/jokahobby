package com.jokahobby.modules.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void markAsRead(List<Notification> notifications) {
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }
}
