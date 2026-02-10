package com.jokahobby.modules.notification;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id", callSuper = false)
public class Notification extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    private String title;

    private String link;

    private String message;

    private boolean checked;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;


}
