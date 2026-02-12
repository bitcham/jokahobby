package com.jokahobby.modules.notification;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String link;

    private String message;

    private boolean checked;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    public void markAsRead() {
        this.checked = true;
    }
}
