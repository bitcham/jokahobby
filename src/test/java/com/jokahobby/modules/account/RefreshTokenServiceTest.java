package com.jokahobby.modules.account;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.infra.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final String TOKEN_HASH = "abc123hash";
    private static final String NEW_TOKEN_HASH = "newHash456";
    private static final String DEVICE_INFO = "TestAgent/1.0";
    private static final String IP_ADDRESS = "127.0.0.1";

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.maxSessions()).thenReturn(5);
        lenient().when(jwtProperties.refreshTokenExpiry()).thenReturn(604800000L);
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("creates token when active count is below maxSessions")
        void createsToken_belowMaxSessions() {
            // Given
            given(refreshTokenRepository.countByAccountIdAndRevokedFalse(ACCOUNT_ID)).willReturn(2L);
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            RefreshToken result = refreshTokenService.createRefreshToken(
                    ACCOUNT_ID, TOKEN_HASH, DEVICE_INFO, IP_ADDRESS);

            // Then
            assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(result.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(result.getGeneration()).isZero();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.getFamilyId()).isNotBlank();

            verify(refreshTokenRepository, never()).revokeFamily(anyString());
        }

        @Test
        @DisplayName("revokes oldest family when maxSessions exceeded")
        void revokesOldestFamily_whenMaxSessionsExceeded() {
            // Given
            given(refreshTokenRepository.countByAccountIdAndRevokedFalse(ACCOUNT_ID)).willReturn(5L);

            RefreshToken oldest = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .familyId("oldest-family")
                    .issuedAt(Instant.now().minus(Duration.ofDays(7)))
                    .build();
            given(refreshTokenRepository.findOldestActiveByAccountId(ACCOUNT_ID))
                    .willReturn(Optional.of(oldest));
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            refreshTokenService.createRefreshToken(ACCOUNT_ID, TOKEN_HASH, DEVICE_INFO, IP_ADDRESS);

            // Then
            verify(refreshTokenRepository).revokeFamily("oldest-family");
        }
    }

    @Nested
    @DisplayName("rotateRefreshToken")
    class RotateRefreshToken {

        @Test
        @DisplayName("successfully rotates a usable token")
        void rotatesUsableToken() {
            // Given
            RefreshToken oldToken = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("family-1")
                    .generation(2)
                    .issuedAt(Instant.now().minus(Duration.ofMinutes(10)))
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(oldToken));
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            RefreshToken newToken = refreshTokenService.rotateRefreshToken(
                    TOKEN_HASH, NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS);

            // Then
            assertThat(oldToken.isRevoked()).isTrue();
            assertThat(oldToken.getReplacedByHash()).isEqualTo(NEW_TOKEN_HASH);

            assertThat(newToken.getTokenHash()).isEqualTo(NEW_TOKEN_HASH);
            assertThat(newToken.getFamilyId()).isEqualTo("family-1");
            assertThat(newToken.getGeneration()).isEqualTo(3);
            assertThat(newToken.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("throws INVALID_TOKEN when token hash not found")
        void throwsInvalidToken_whenNotFound() {
            // Given
            given(refreshTokenRepository.findByTokenHash("unknown-hash")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    refreshTokenService.rotateRefreshToken("unknown-hash", NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("throws REFRESH_TOKEN_EXPIRED when token is expired")
        void throwsExpired_whenTokenExpired() {
            // Given - revoked=false but expired
            RefreshToken expiredToken = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("family-1")
                    .generation(0)
                    .issuedAt(Instant.now().minus(Duration.ofDays(10)))
                    .expiresAt(Instant.now().minus(Duration.ofDays(3)))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(expiredToken));

            // When & Then
            assertThatThrownBy(() ->
                    refreshTokenService.rotateRefreshToken(TOKEN_HASH, NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("detects replay attack and revokes entire family")
        void detectsReplayAttack_revokesFamily() {
            // Given - already revoked, replacedByHash set, but grace window expired
            RefreshToken revokedToken = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("compromised-family")
                    .generation(1)
                    .issuedAt(Instant.now().minus(Duration.ofMinutes(30)))
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(true)
                    .replacedByHash("replaced-hash")
                    .build();

            RefreshToken replacement = RefreshToken.builder()
                    .tokenHash("replaced-hash")
                    .issuedAt(Instant.now().minus(Duration.ofMinutes(10))) // well past grace window
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(revokedToken));
            given(refreshTokenRepository.findByTokenHash("replaced-hash")).willReturn(Optional.of(replacement));

            // When & Then
            assertThatThrownBy(() ->
                    refreshTokenService.rotateRefreshToken(TOKEN_HASH, NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

            verify(refreshTokenRepository).revokeFamily("compromised-family");
        }

        @Test
        @DisplayName("allows idempotent request within grace window")
        void allowsIdempotent_withinGraceWindow() {
            // Given - revoked token with replacement created just now (within 5s)
            RefreshToken revokedToken = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("family-1")
                    .generation(1)
                    .issuedAt(Instant.now().minus(Duration.ofSeconds(2)))
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(true)
                    .replacedByHash("grace-replacement-hash")
                    .build();

            RefreshToken replacement = RefreshToken.builder()
                    .tokenHash("grace-replacement-hash")
                    .familyId("family-1")
                    .generation(2)
                    .accountId(ACCOUNT_ID)
                    .issuedAt(Instant.now().minus(Duration.ofSeconds(1))) // within 5s grace
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(revokedToken));
            given(refreshTokenRepository.findByTokenHash("grace-replacement-hash")).willReturn(Optional.of(replacement));

            // When
            RefreshToken result = refreshTokenService.rotateRefreshToken(
                    TOKEN_HASH, NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS);

            // Then - returns existing replacement, doesn't revoke family
            assertThat(result.getTokenHash()).isEqualTo("grace-replacement-hash");
            verify(refreshTokenRepository, never()).revokeFamily(anyString());
        }

        @Test
        @DisplayName("revokes family when revoked token has no replacement hash")
        void revokesFamily_whenNoReplacementHash() {
            // Given - revoked but no replacedByHash (manually revoked)
            RefreshToken revokedToken = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("family-manual")
                    .generation(0)
                    .issuedAt(Instant.now().minus(Duration.ofMinutes(5)))
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .revoked(true)
                    .replacedByHash(null)
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(revokedToken));

            // When & Then
            assertThatThrownBy(() ->
                    refreshTokenService.rotateRefreshToken(TOKEN_HASH, NEW_TOKEN_HASH, DEVICE_INFO, IP_ADDRESS))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

            verify(refreshTokenRepository).revokeFamily("family-manual");
        }
    }

    @Nested
    @DisplayName("revokeToken")
    class RevokeToken {

        @Test
        @DisplayName("revokes existing token")
        void revokesExistingToken() {
            // Given
            RefreshToken token = RefreshToken.builder()
                    .accountId(ACCOUNT_ID)
                    .tokenHash(TOKEN_HASH)
                    .familyId("family-1")
                    .revoked(false)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                    .build();

            given(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).willReturn(Optional.of(token));

            // When
            refreshTokenService.revokeToken(TOKEN_HASH);

            // Then
            assertThat(token.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("does nothing when token not found")
        void doesNothing_whenNotFound() {
            // Given
            given(refreshTokenRepository.findByTokenHash("nonexistent")).willReturn(Optional.empty());

            // When & Then - no exception
            refreshTokenService.revokeToken("nonexistent");
        }
    }

    @Nested
    @DisplayName("revokeAllTokens")
    class RevokeAllTokens {

        @Test
        @DisplayName("revokes all tokens for account")
        void revokesAll() {
            // Given
            given(refreshTokenRepository.revokeAllByAccountId(ACCOUNT_ID)).willReturn(3);

            // When
            refreshTokenService.revokeAllTokens(ACCOUNT_ID);

            // Then
            verify(refreshTokenRepository).revokeAllByAccountId(ACCOUNT_ID);
        }
    }
}
