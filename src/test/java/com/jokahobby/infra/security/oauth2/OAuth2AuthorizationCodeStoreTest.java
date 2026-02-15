package com.jokahobby.infra.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2AuthorizationCodeStoreTest {

    private Clock clock;
    private Instant baseTime;
    private OAuth2AuthorizationCodeStore store;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2025-01-01T00:00:00Z");
        clock = Clock.fixed(baseTime, ZoneId.of("UTC"));
        store = new OAuth2AuthorizationCodeStore();
        ReflectionTestUtils.setField(store, "clock", clock);
        ReflectionTestUtils.setField(store, "codeTtlSeconds", 30L);
    }

    private void advanceClock(Duration duration) {
        clock = Clock.fixed(baseTime.plus(duration), ZoneId.of("UTC"));
        ReflectionTestUtils.setField(store, "clock", clock);
    }

    @DisplayName("Store and consume with valid binding hash returns data")
    @Test
    void storeAndConsume_validBinding_returnsData() {
        UUID accountId = UUID.randomUUID();
        String bindingHash = "validhash123";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                accountId, true, "TestAgent", "127.0.0.1", bindingHash, baseTime);

        String code = store.store(data);

        OAuth2AuthorizationData result = store.consumeIfValid(code, bindingHash);
        assertThat(result).isNotNull();
        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(result.nicknameRequired()).isTrue();
    }

    @DisplayName("Double consumption returns null")
    @Test
    void doubleConsumption_returnsNull() {
        String bindingHash = "hash";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", bindingHash, baseTime);

        String code = store.store(data);
        store.consumeIfValid(code, bindingHash);

        assertThat(store.consumeIfValid(code, bindingHash)).isNull();
    }

    @DisplayName("Unknown code returns null")
    @Test
    void unknownCode_returnsNull() {
        assertThat(store.consumeIfValid("nonexistent-code", "anyhash")).isNull();
    }

    @DisplayName("Expired code is rejected (31 seconds)")
    @Test
    void expiredCode_rejected() {
        String bindingHash = "hash";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", bindingHash, baseTime);

        String code = store.store(data);
        advanceClock(Duration.ofSeconds(31));

        assertThat(store.consumeIfValid(code, bindingHash)).isNull();
    }

    @DisplayName("Code at exactly 30 seconds is still valid")
    @Test
    void ttlBoundary_exactlyAtLimit_valid() {
        String bindingHash = "hash";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", bindingHash, baseTime);

        String code = store.store(data);
        advanceClock(Duration.ofSeconds(30));

        assertThat(store.consumeIfValid(code, bindingHash)).isNotNull();
    }

    @DisplayName("Code at 30 seconds + 1 millisecond is expired")
    @Test
    void ttlBoundary_justPastLimit_expired() {
        String bindingHash = "hash";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", bindingHash, baseTime);

        String code = store.store(data);
        advanceClock(Duration.ofSeconds(30).plusMillis(1));

        assertThat(store.consumeIfValid(code, bindingHash)).isNull();
    }

    @DisplayName("Binding mismatch does not consume code")
    @Test
    void bindingMismatch_doesNotConsumeCode() {
        String correctHash = "correcthash";
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", correctHash, baseTime);

        String code = store.store(data);

        // Wrong binding should fail
        assertThat(store.consumeIfValid(code, "wronghash")).isNull();

        // Code should still exist (not consumed)
        assertThat(store.consumeIfValid(code, correctHash)).isNotNull();
    }

    @DisplayName("Binding mismatch then valid retry succeeds")
    @Test
    void bindingMismatch_thenValidRetry_succeeds() {
        String correctHash = "correcthash";
        UUID accountId = UUID.randomUUID();
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                accountId, true, "Agent", "127.0.0.1", correctHash, baseTime);

        String code = store.store(data);

        // Multiple wrong attempts
        store.consumeIfValid(code, "wrong1");
        store.consumeIfValid(code, "wrong2");

        // Correct attempt still works
        OAuth2AuthorizationData result = store.consumeIfValid(code, correctHash);
        assertThat(result).isNotNull();
        assertThat(result.accountId()).isEqualTo(accountId);
    }

    @DisplayName("purgeExpired removes only expired entries")
    @Test
    void purgeExpired_removesOnlyExpired() {
        String hash1 = "hash1";
        String hash2 = "hash2";
        OAuth2AuthorizationData data1 = new OAuth2AuthorizationData(
                UUID.randomUUID(), false, "Agent", "127.0.0.1", hash1, baseTime);
        String code1 = store.store(data1);

        // Advance 20 seconds and store another code
        advanceClock(Duration.ofSeconds(20));
        OAuth2AuthorizationData data2 = new OAuth2AuthorizationData(
                UUID.randomUUID(), true, "Agent", "127.0.0.1", hash2, clock.instant());
        String code2 = store.store(data2);

        // Advance to 31 seconds from base (code1 expired, code2 still valid)
        advanceClock(Duration.ofSeconds(31));
        store.purgeExpired();

        assertThat(store.consumeIfValid(code1, hash1)).isNull();
        assertThat(store.consumeIfValid(code2, hash2)).isNotNull();
    }
}
