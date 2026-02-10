package com.jokahobby.modules.account;

import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false)
    private int generation;

    @Column(name = "device_info", length = 256)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isUsable() {
        return !this.revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public void replaceWith(String newTokenHash) {
        this.revoked = true;
        this.replacedByHash = newTokenHash;
    }

    public boolean wasRecentlyReplaced(long graceWindowSeconds) {
        if (!this.revoked || this.replacedByHash == null) {
            return false;
        }
        return Duration.between(this.issuedAt, Instant.now()).toSeconds() < graceWindowSeconds;
    }
}
