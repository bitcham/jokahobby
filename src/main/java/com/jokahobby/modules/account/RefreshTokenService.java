package com.jokahobby.modules.account;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.infra.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final long GRACE_WINDOW_SECONDS = 5;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken createRefreshToken(UUID accountId, String tokenHash,
                                           String deviceInfo, String ipAddress) {
        long activeCount = refreshTokenRepository.countByAccountIdAndRevokedFalse(accountId);
        if (activeCount >= jwtProperties.maxSessions()) {
            refreshTokenRepository.findOldestActiveByAccountId(accountId)
                    .ifPresent(oldest -> {
                        refreshTokenRepository.revokeFamily(oldest.getFamilyId());
                        log.info("Max sessions exceeded for accountId={}, revoked family={}", accountId, oldest.getFamilyId());
                    });
        }

        Instant now = Instant.now();
        RefreshToken rt = RefreshToken.builder()
                .accountId(accountId)
                .tokenHash(tokenHash)
                .familyId(UUID.randomUUID().toString())
                .generation(0)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMillis(jwtProperties.refreshTokenExpiry())))
                .revoked(false)
                .build();

        log.info("Refresh token created for accountId={}, family={}", accountId, rt.getFamilyId());
        return refreshTokenRepository.save(rt);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenHash, String newTokenHash,
                                           String deviceInfo, String ipAddress) {
        RefreshToken old = refreshTokenRepository.findByTokenHash(oldTokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (old.isUsable()) {
            old.replaceWith(newTokenHash);
            Instant now = Instant.now();
            RefreshToken newRt = RefreshToken.builder()
                    .accountId(old.getAccountId())
                    .tokenHash(newTokenHash)
                    .familyId(old.getFamilyId())
                    .generation(old.getGeneration() + 1)
                    .deviceInfo(deviceInfo)
                    .ipAddress(ipAddress)
                    .issuedAt(now)
                    .expiresAt(now.plus(Duration.ofMillis(jwtProperties.refreshTokenExpiry())))
                    .revoked(false)
                    .build();

            log.debug("Token rotated for accountId={}, family={}, gen={}", old.getAccountId(), old.getFamilyId(), newRt.getGeneration());
            return refreshTokenRepository.save(newRt);
        }

        if (old.isRevoked()) {
            if (old.getReplacedByHash() != null) {
                Optional<RefreshToken> replacement = refreshTokenRepository
                        .findByTokenHash(old.getReplacedByHash());
                if (replacement.isPresent() && replacement.get().isUsable()
                        && Duration.between(replacement.get().getIssuedAt(), Instant.now()).toSeconds() < GRACE_WINDOW_SECONDS) {
                    log.debug("Idempotent refresh for family={}", old.getFamilyId());
                    return replacement.get();
                }
            }

            refreshTokenRepository.revokeFamily(old.getFamilyId());
            log.warn("Replay attack detected for accountId={}, family={}", old.getAccountId(), old.getFamilyId());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Transactional
    public void revokeToken(String tokenHash) {
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(rt -> {
                    rt.revoke();
                    log.info("Token revoked for accountId={}, family={}", rt.getAccountId(), rt.getFamilyId());
                });
    }

    @Transactional
    public void revokeAllTokens(UUID accountId) {
        int count = refreshTokenRepository.revokeAllByAccountId(accountId);
        log.info("All tokens revoked for accountId={}, count={}", accountId, count);
    }
}
