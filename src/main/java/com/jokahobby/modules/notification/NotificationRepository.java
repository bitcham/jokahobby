package com.jokahobby.modules.notification;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByAccountAndChecked(Account account, boolean checked);

    List<Notification> findByAccountAndCheckedOrderByCreatedAtDesc(Account account, boolean checked);

    void deleteByAccountAndChecked(Account account, boolean checked);
}
