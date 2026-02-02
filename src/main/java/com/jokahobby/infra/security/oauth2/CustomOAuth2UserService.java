package com.jokahobby.infra.security.oauth2;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");

        Account account = accountRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createAccount(provider, providerId, email));

        return new OAuth2UserPrincipal(account, attributes);
    }

    private Account createAccount(String provider, String providerId, String email) {
        Account account = Account.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .joinedAt(LocalDateTime.now())
                .build();

        Account saved = accountRepository.save(account);
        log.info("New account created via OAuth2: provider={}, accountId={}", provider, saved.getId());
        return saved;
    }
}
