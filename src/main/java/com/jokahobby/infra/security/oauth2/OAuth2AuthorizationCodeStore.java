package com.jokahobby.infra.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class OAuth2AuthorizationCodeStore {

    @Value("${app.oauth2-code-ttl:30}")
    private long codeTtlSeconds;

    private final ConcurrentHashMap<String, OAuth2AuthorizationData> store = new ConcurrentHashMap<>();
    private Clock clock;

    public OAuth2AuthorizationCodeStore() {
        this.clock = Clock.systemUTC();
    }

    public String store(OAuth2AuthorizationData data) {
        String code = UUID.randomUUID().toString();
        store.put(code, data);
        return code;
    }

    public OAuth2AuthorizationData consumeIfValid(String code, String bindingHash) {
        OAuth2AuthorizationData data = store.get(code);
        if (data == null) {
            return null;
        }

        if (isExpired(data)) {
            store.remove(code, data);
            return null;
        }

        if (!data.bindingHash().equals(bindingHash)) {
            return null;
        }

        if (store.remove(code, data)) {
            return data;
        }

        return null;
    }

    @Scheduled(fixedRate = 60_000)
    public void purgeExpired() {
        store.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private boolean isExpired(OAuth2AuthorizationData data) {
        return Duration.between(data.createdAt(), clock.instant()).compareTo(Duration.ofSeconds(codeTtlSeconds)) > 0;
    }
}
