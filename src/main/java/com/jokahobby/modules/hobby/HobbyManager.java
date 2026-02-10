package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id", "account_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
public class HobbyManager extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Builder
    private HobbyManager(Hobby hobby, Account account) {
        this.hobby = hobby;
        this.account = account;
    }
}
