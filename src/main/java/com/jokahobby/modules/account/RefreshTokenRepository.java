package com.jokahobby.modules.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long countByAccountIdAndRevokedFalse(UUID accountId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId")
    int revokeFamily(@Param("familyId") String familyId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.accountId = :accountId")
    int revokeAllByAccountId(@Param("accountId") UUID accountId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT rt FROM RefreshToken rt
            WHERE rt.accountId = :accountId AND rt.revoked = false
            ORDER BY rt.issuedAt ASC
            LIMIT 1
            """)
    Optional<RefreshToken> findOldestActiveByAccountId(@Param("accountId") UUID accountId);
}
