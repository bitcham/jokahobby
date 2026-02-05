package com.jokahobby.infra.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhMjU2LWFsZ29yaXRobQ==";
    private static final long ACCESS_TOKEN_EXPIRY = 1800000L;   // 30 min
    private static final long REFRESH_TOKEN_EXPIRY = 604800000L; // 7 days

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                TEST_SECRET, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY, 5, false);
        jwtProvider = new JwtProvider(props);
    }

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("returns a valid JWT with accountId as subject")
        void createsValidToken() {
            UUID accountId = UUID.randomUUID();

            String token = jwtProvider.createAccessToken(accountId);

            assertThat(token).isNotBlank();
            assertThat(jwtProvider.validateToken(token)).isTrue();
            assertThat(jwtProvider.getAccountId(token)).isEqualTo(accountId);
        }

        @Test
        @DisplayName("includes 'access' type claim")
        void includesTypeClaim() {
            UUID accountId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(accountId);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            String type = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload()
                    .get("type", String.class);

            assertThat(type).isEqualTo("access");
        }
    }

    @Nested
    @DisplayName("createRefreshToken")
    class CreateRefreshToken {

        @Test
        @DisplayName("returns a valid JWT with family and generation claims")
        void createsValidRefreshToken() {
            UUID accountId = UUID.randomUUID();
            String familyId = UUID.randomUUID().toString();
            int generation = 3;

            String token = jwtProvider.createRefreshToken(accountId, familyId, generation);

            assertThat(token).isNotBlank();
            assertThat(jwtProvider.validateToken(token)).isTrue();
            assertThat(jwtProvider.getAccountId(token)).isEqualTo(accountId);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
            assertThat(claims.get("family", String.class)).isEqualTo(familyId);
            assertThat(claims.get("gen", Integer.class)).isEqualTo(generation);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("returns true for valid token")
        void validToken() {
            String token = jwtProvider.createAccessToken(UUID.randomUUID());
            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for tampered token")
        void tamperedToken() {
            String token = jwtProvider.createAccessToken(UUID.randomUUID());
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThat(jwtProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("returns false for expired token")
        void expiredToken() {
            // Create a provider with 0ms expiry to produce already-expired tokens
            JwtProperties expiredProps = new JwtProperties(TEST_SECRET, 0L, 0L, 5, false);
            JwtProvider expiredProvider = new JwtProvider(expiredProps);

            String token = expiredProvider.createAccessToken(UUID.randomUUID());
            assertThat(expiredProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("returns false for null token")
        void nullToken() {
            assertThat(jwtProvider.validateToken(null)).isFalse();
        }

        @Test
        @DisplayName("returns false for empty token")
        void emptyToken() {
            assertThat(jwtProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("returns false for completely invalid string")
        void invalidString() {
            assertThat(jwtProvider.validateToken("not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("returns false for token signed with different key")
        void differentKey() {
            String differentSecret = "ZGlmZmVyZW50LXNlY3JldC1rZXktdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTYtYWxnbw==";
            JwtProperties otherProps = new JwtProperties(differentSecret, ACCESS_TOKEN_EXPIRY, REFRESH_TOKEN_EXPIRY, 5, false);
            JwtProvider otherProvider = new JwtProvider(otherProps);

            String token = otherProvider.createAccessToken(UUID.randomUUID());
            assertThat(jwtProvider.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("getAccountId")
    class GetAccountId {

        @Test
        @DisplayName("extracts correct accountId from access token")
        void fromAccessToken() {
            UUID accountId = UUID.randomUUID();
            String token = jwtProvider.createAccessToken(accountId);
            assertThat(jwtProvider.getAccountId(token)).isEqualTo(accountId);
        }

        @Test
        @DisplayName("extracts correct accountId from refresh token")
        void fromRefreshToken() {
            UUID accountId = UUID.randomUUID();
            String token = jwtProvider.createRefreshToken(accountId, "family-1", 0);
            assertThat(jwtProvider.getAccountId(token)).isEqualTo(accountId);
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpirySeconds")
    class GetExpirySeconds {

        @Test
        @DisplayName("returns expiry in seconds")
        void returnsSeconds() {
            assertThat(jwtProvider.getAccessTokenExpirySeconds())
                    .isEqualTo(ACCESS_TOKEN_EXPIRY / 1000);
        }
    }
}
