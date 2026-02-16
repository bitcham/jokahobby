package com.jokahobby.infra.security.jwt;

import com.jokahobby.infra.security.oauth2.OAuth2UserPrincipal;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                UUID accountId = jwtProvider.getAccountId(token);
                Account account = accountRepository.findById(accountId).orElse(null);

                if (account != null) {
                    MDC.put("accountId", accountId.toString());
                    OAuth2UserPrincipal principal = new OAuth2UserPrincipal(account, Map.of());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated accountId={}", accountId);
                }
            } catch (SignatureException e) {
                log.warn("Token signature mismatch: {}", e.getMessage());
            } catch (ExpiredJwtException e) {
                log.debug("Token expired: {}", e.getMessage());
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Token validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
