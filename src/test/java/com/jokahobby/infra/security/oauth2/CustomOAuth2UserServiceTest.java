package com.jokahobby.infra.security.oauth2;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class CustomOAuth2UserServiceTest extends AbstractContainerBaseTest {

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @DisplayName("findByProviderAndProviderId returns existing account")
    @Test
    void findExistingAccount() {
        Account account = Account.builder()
                .provider("GOOGLE")
                .providerId("google-sub-123")
                .email("test@gmail.com")
                .nickname("testuser")
                .joinedAt(LocalDateTime.now())
                .build();
        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByProviderAndProviderId("GOOGLE", "google-sub-123");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@gmail.com");
    }

    @DisplayName("findByProviderAndProviderId returns empty for unknown provider ID")
    @Test
    void findNonExistingAccount() {
        Optional<Account> found = accountRepository.findByProviderAndProviderId("GOOGLE", "unknown-sub");
        assertThat(found).isEmpty();
    }

    @DisplayName("New account created with null nickname (for frontend to prompt)")
    @Test
    void createAccountWithNullNickname() {
        Account account = Account.builder()
                .provider("GOOGLE")
                .providerId("new-sub-456")
                .email("newuser@gmail.com")
                .joinedAt(LocalDateTime.now())
                .build();
        Account saved = accountRepository.save(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNickname()).isNull();
        assertThat(saved.getProvider()).isEqualTo("GOOGLE");
        assertThat(saved.getProviderId()).isEqualTo("new-sub-456");
    }

    @DisplayName("OAuth2UserPrincipal wraps account correctly")
    @Test
    void oAuth2UserPrincipalWrapsAccount() {
        Account account = Account.builder()
                .provider("GOOGLE")
                .providerId("sub-789")
                .email("wrap@gmail.com")
                .nickname("wrapuser")
                .joinedAt(LocalDateTime.now())
                .build();
        accountRepository.save(account);

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(account, java.util.Map.of());
        assertThat(principal.getAccount()).isEqualTo(account);
        assertThat(principal.getName()).isEqualTo(account.getId().toString());
        assertThat(principal.getAuthorities()).hasSize(1);
    }
}
