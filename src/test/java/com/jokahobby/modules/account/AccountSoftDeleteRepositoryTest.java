package com.jokahobby.modules.account;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class AccountSoftDeleteRepositoryTest extends AbstractContainerBaseTest {

    @Autowired AccountRepository accountRepository;
    @Autowired EntityManager em;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    private Account createAccount(String nickname) {
        return accountRepository.save(Account.builder()
                .email(nickname + "@test.com")
                .nickname(nickname)
                .provider("google")
                .providerId("google-" + nickname)
                .joinedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("soft-deleted account is excluded from findByNickname")
    void findByNicknameExcludesSoftDeleted() {
        Account account = createAccount("testuser");
        account.softDelete();
        em.flush();
        em.clear();

        assertThat(accountRepository.findByNickname("testuser")).isNull();
    }

    @Test
    @DisplayName("soft-deleted account is excluded from existsByNickname")
    void existsByNicknameExcludesSoftDeleted() {
        Account account = createAccount("testuser");
        account.softDelete();
        em.flush();
        em.clear();

        assertThat(accountRepository.existsByNickname("testuser")).isFalse();
    }

    @Test
    @DisplayName("can create account with same nickname after soft delete")
    void canReuseNickname() {
        Account account = createAccount("reuse");
        account.softDelete();
        em.flush();
        em.clear();

        Account newAccount = createAccount("reuse");
        assertThat(newAccount.getId()).isNotNull();
    }
}
