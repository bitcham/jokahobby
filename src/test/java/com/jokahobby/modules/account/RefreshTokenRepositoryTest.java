package com.jokahobby.modules.account;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class RefreshTokenRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String FAMILY_A = "family-a";
    private static final String FAMILY_B = "family-b";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
    }

    private RefreshToken saveToken(UUID accountId, String familyId, String hash,
                                   boolean revoked, Instant issuedAt, Instant expiresAt) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .accountId(accountId)
                .tokenHash(hash)
                .familyId(familyId)
                .generation(0)
                .deviceInfo("TestAgent")
                .ipAddress("127.0.0.1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build());
    }

    @Nested
    @DisplayName("findByTokenHash")
    class FindByTokenHash {

        @Test
        @DisplayName("returns token for matching hash")
        void returnsMatch() {
            saveToken(ACCOUNT_ID, FAMILY_A, "hash-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-1");

            assertThat(found).isPresent();
            assertThat(found.get().getAccountId()).isEqualTo(ACCOUNT_ID);
        }

        @Test
        @DisplayName("returns empty for non-existing hash")
        void returnsEmpty() {
            Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("nonexistent");
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByAccountIdAndRevokedFalse")
    class CountActive {

        @Test
        @DisplayName("counts only active (non-revoked) tokens")
        void countsOnlyActive() {
            saveToken(ACCOUNT_ID, FAMILY_A, "active-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));
            saveToken(ACCOUNT_ID, FAMILY_A, "active-2", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));
            saveToken(ACCOUNT_ID, FAMILY_B, "revoked-1", true,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            long count = refreshTokenRepository.countByAccountIdAndRevokedFalse(ACCOUNT_ID);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("returns zero when no active tokens")
        void returnsZero() {
            long count = refreshTokenRepository.countByAccountIdAndRevokedFalse(UUID.randomUUID());
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("revokeFamily")
    class RevokeFamily {

        @Test
        @DisplayName("revokes all tokens in the same family")
        void revokesAllInFamily() {
            saveToken(ACCOUNT_ID, FAMILY_A, "fam-a-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));
            saveToken(ACCOUNT_ID, FAMILY_A, "fam-a-2", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));
            saveToken(ACCOUNT_ID, FAMILY_B, "fam-b-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            int revoked = refreshTokenRepository.revokeFamily(FAMILY_A);

            assertThat(revoked).isEqualTo(2);

            // @Transactional test에서 JPQL UPDATE 후 영속성 컨텍스트 재조회를 위해 flush 후 확인
            long activeA = refreshTokenRepository.countByAccountIdAndRevokedFalse(ACCOUNT_ID);
            // family-b-1만 active
            assertThat(activeA).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("revokeAllByAccountId")
    class RevokeAllByAccountId {

        @Test
        @DisplayName("revokes all tokens for account across families")
        void revokesAllForAccount() {
            saveToken(ACCOUNT_ID, FAMILY_A, "all-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));
            saveToken(ACCOUNT_ID, FAMILY_B, "all-2", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            UUID otherId = UUID.randomUUID();
            saveToken(otherId, "other-family", "other-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            int revoked = refreshTokenRepository.revokeAllByAccountId(ACCOUNT_ID);

            assertThat(revoked).isEqualTo(2);

            // Other account's token should still be active
            long otherActive = refreshTokenRepository.countByAccountIdAndRevokedFalse(otherId);
            assertThat(otherActive).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteExpired")
    class DeleteExpired {

        @Test
        @DisplayName("deletes tokens expired before cutoff")
        void deletesExpiredTokens() {
            saveToken(ACCOUNT_ID, FAMILY_A, "expired-1", false,
                    Instant.now().minus(Duration.ofDays(10)), Instant.now().minus(Duration.ofDays(3)));
            saveToken(ACCOUNT_ID, FAMILY_A, "expired-2", true,
                    Instant.now().minus(Duration.ofDays(10)), Instant.now().minus(Duration.ofDays(1)));
            saveToken(ACCOUNT_ID, FAMILY_B, "valid-1", false,
                    Instant.now(), Instant.now().plus(Duration.ofDays(7)));

            int deleted = refreshTokenRepository.deleteExpired(Instant.now());

            assertThat(deleted).isEqualTo(2);
            assertThat(refreshTokenRepository.findByTokenHash("valid-1")).isPresent();
            assertThat(refreshTokenRepository.findByTokenHash("expired-1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOldestActiveByAccountId")
    class FindOldestActive {

        @Test
        @DisplayName("returns the oldest non-revoked token")
        void returnsOldest() {
            saveToken(ACCOUNT_ID, FAMILY_A, "oldest", false,
                    Instant.now().minus(Duration.ofDays(5)), Instant.now().plus(Duration.ofDays(2)));
            saveToken(ACCOUNT_ID, FAMILY_B, "newer", false,
                    Instant.now().minus(Duration.ofDays(1)), Instant.now().plus(Duration.ofDays(6)));
            saveToken(ACCOUNT_ID, "family-c", "revoked-old", true,
                    Instant.now().minus(Duration.ofDays(10)), Instant.now().plus(Duration.ofDays(7)));

            Optional<RefreshToken> oldest = refreshTokenRepository.findOldestActiveByAccountId(ACCOUNT_ID);

            assertThat(oldest).isPresent();
            assertThat(oldest.get().getTokenHash()).isEqualTo("oldest");
        }

        @Test
        @DisplayName("returns empty when no active tokens")
        void returnsEmpty_whenNoActive() {
            saveToken(ACCOUNT_ID, FAMILY_A, "revoked-only", true,
                    Instant.now().minus(Duration.ofDays(1)), Instant.now().plus(Duration.ofDays(7)));

            Optional<RefreshToken> oldest = refreshTokenRepository.findOldestActiveByAccountId(ACCOUNT_ID);

            assertThat(oldest).isEmpty();
        }
    }
}
