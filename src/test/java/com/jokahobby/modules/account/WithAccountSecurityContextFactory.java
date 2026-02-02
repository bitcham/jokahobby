package com.jokahobby.modules.account;

import com.jokahobby.infra.security.oauth2.OAuth2UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class WithAccountSecurityContextFactory implements WithSecurityContextFactory<WithAccount> {

    private final AccountRepository accountRepository;

    @Override
    public SecurityContext createSecurityContext(WithAccount annotation) {
        String nickname = annotation.value();

        Account account = Account.builder()
                .nickname(nickname)
                .email(nickname + "@email.com")
                .provider("GOOGLE")
                .providerId("test-" + nickname)
                .joinedAt(LocalDateTime.now())
                .build();
        accountRepository.save(account);

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(account, Map.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
