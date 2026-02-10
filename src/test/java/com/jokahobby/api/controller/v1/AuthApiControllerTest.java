package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.account.RefreshToken;
import com.jokahobby.modules.account.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class AuthApiControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired AccountRepository accountRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();

        testAccount = accountRepository.save(Account.builder()
                .email("auth@example.com")
                .nickname("authuser")
                .provider("GOOGLE")
                .providerId("google-auth-123")
                .joinedAt(Instant.now())
                .build());
    }

    private String createRefreshTokenRaw(String familyId, int generation) {
        return jwtProvider.createRefreshToken(testAccount.getId(), familyId, generation);
    }

    private RefreshToken storeRefreshToken(String rawToken, String familyId) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .accountId(testAccount.getId())
                .tokenHash(TokenHashUtil.sha256(rawToken))
                .familyId(familyId)
                .generation(0)
                .deviceInfo("TestAgent")
                .ipAddress("127.0.0.1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .revoked(false)
                .build());
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("returns new access token on successful refresh")
        void refreshSuccess() {
            String familyId = UUID.randomUUID().toString();
            String rawRefreshToken = createRefreshTokenRaw(familyId, 0);
            storeRefreshToken(rawRefreshToken, familyId);

            var result = mockMvc.post().uri("/api/v1/auth/refresh")
                    .cookie(new Cookie("refreshToken", rawRefreshToken))
                    .header("User-Agent", "TestAgent");

            assertThat(result)
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.data.accessToken").isNotNull();

            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.data.expiresIn").isNotNull();
        }

        @Test
        @DisplayName("returns 400 when refresh token cookie is missing")
        void refreshWithoutCookie() {
            assertThat(mockMvc.post().uri("/api/v1/auth/refresh"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 401 when refresh token is invalid JWT")
        void refreshWithInvalidToken() {
            assertThat(mockMvc.post().uri("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", "invalid.jwt.token")))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("returns 401 when refresh token is not in database")
        void refreshWithUnknownToken() {
            String rawToken = createRefreshTokenRaw(UUID.randomUUID().toString(), 0);
            // Don't store in DB

            assertThat(mockMvc.post().uri("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", rawToken)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("revokes token and deletes cookie")
        void logoutSuccess() {
            String familyId = UUID.randomUUID().toString();
            String rawRefreshToken = createRefreshTokenRaw(familyId, 0);
            storeRefreshToken(rawRefreshToken, familyId);

            var result = mockMvc.post().uri("/api/v1/auth/logout")
                    .cookie(new Cookie("refreshToken", rawRefreshToken));

            assertThat(result)
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            // Verify token is revoked in DB
            String hash = TokenHashUtil.sha256(rawRefreshToken);
            assertThat(refreshTokenRepository.findByTokenHash(hash).get().isRevoked()).isTrue();
        }

        @Test
        @DisplayName("succeeds even without refresh token cookie")
        void logoutWithoutCookie() {
            assertThat(mockMvc.post().uri("/api/v1/auth/logout"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout-all")
    class LogoutAll {

        @Test
        @DisplayName("revokes all tokens for account")
        void logoutAllSuccess() {
            // Use different familyIds and generations to ensure unique tokens
            String family1 = UUID.randomUUID().toString();
            String family2 = UUID.randomUUID().toString();
            String rawRefreshToken1 = createRefreshTokenRaw(family1, 0);
            String rawRefreshToken2 = createRefreshTokenRaw(family2, 1);

            storeRefreshToken(rawRefreshToken1, family1);
            storeRefreshToken(rawRefreshToken2, family2);

            String accessToken = jwtProvider.createAccessToken(testAccount.getId());

            var result = mockMvc.post().uri("/api/v1/auth/logout-all")
                    .header("Authorization", "Bearer " + accessToken)
                    .cookie(new Cookie("refreshToken", rawRefreshToken1));

            assertThat(result)
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            // Verify all tokens revoked
            long activeCount = refreshTokenRepository.countByAccountIdAndRevokedFalse(testAccount.getId());
            assertThat(activeCount).isZero();
        }

        @Test
        @DisplayName("returns 401 without Bearer token")
        void logoutAllWithoutBearer() {
            String rawToken = createRefreshTokenRaw(UUID.randomUUID().toString(), 0);
            assertThat(mockMvc.post().uri("/api/v1/auth/logout-all")
                            .cookie(new Cookie("refreshToken", rawToken)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("returns 401 with invalid Bearer token")
        void logoutAllWithInvalidBearer() {
            String rawToken = createRefreshTokenRaw(UUID.randomUUID().toString(), 0);
            assertThat(mockMvc.post().uri("/api/v1/auth/logout-all")
                            .header("Authorization", "Bearer invalid.token")
                            .cookie(new Cookie("refreshToken", rawToken)))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
