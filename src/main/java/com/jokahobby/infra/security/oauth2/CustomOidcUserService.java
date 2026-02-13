package com.jokahobby.infra.security.oauth2;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Map<String, Object> attributes = oidcUser.getAttributes();
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");

        Account account = accountRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createAccount(provider, providerId, email));

        return new OAuth2UserPrincipal(account, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private Account createAccount(String provider, String providerId, String email) {
        Account account = Account.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .joinedAt(Instant.now())
                .build();

        Account saved = accountRepository.save(account);
        log.info("New account created via OIDC: provider={}, accountId={}", provider, saved.getId());
        return saved;
    }
}
