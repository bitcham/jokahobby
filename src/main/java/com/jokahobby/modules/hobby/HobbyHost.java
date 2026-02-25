package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
public class HobbyHost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_by")
    private Account transferredBy;

    @Builder
    private HobbyHost(Hobby hobby, Account account) {
        this.hobby = hobby;
        this.account = account;
    }

    public void transferTo(Account newHost, Account previousHost) {
        this.transferredBy = previousHost;
        this.account = newHost;
    }
}
