package com.jokahobby.infra.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(props.secret()));
        this.accessTokenExpiry = props.accessTokenExpiry();
        this.refreshTokenExpiry = props.refreshTokenExpiry();
    }

    public String createAccessToken(UUID accountId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiry))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID accountId, String familyId, int generation) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("type", "refresh")
                .claim("family", familyId)
                .claim("gen", generation)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiry))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UUID getAccountId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiry / 1000;
    }
}
