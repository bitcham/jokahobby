package com.jokahobby.modules.hobby;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MockMvcTest
class HobbyManagerMemberTest extends AbstractContainerBaseTest {

    @Autowired HobbyRepository hobbyRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyMemberRepository hobbyMemberRepository;

    private Hobby hobby;
    private Account creator;
    private Account user;

    @BeforeEach
    void setUp() {
        hobbyManagerRepository.deleteAll();
        hobbyMemberRepository.deleteAll();
        creator = accountRepository.save(Account.builder()
                .email("creator@example.com")
                .nickname("creator")
                .provider("google")
                .providerId("google-creator")
                .joinedAt(Instant.now())
                .build());
        user = accountRepository.save(Account.builder()
                .email("user@example.com")
                .nickname("user1")
                .provider("google")
                .providerId("google-user")
                .joinedAt(Instant.now())
                .build());
        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .published(true)
                .recruiting(true)
                .build());
    }

    @Nested
    @DisplayName("HobbyManager")
    class HobbyManagerTests {

        @Test
        @DisplayName("saves HobbyManager with audit columns")
        void savesWithAudit() {
            HobbyManager hm = hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby)
                    .account(creator)
                                        .build());

            assertThat(hm.getId()).isNotNull();
            assertThat(hm.getCreatedAt()).isNotNull();
            assertThat(hm.getAccount().getId()).isEqualTo(creator.getId());
        }

        @Test
        @DisplayName("rejects duplicate hobby-account pair")
        void rejectsDuplicate() {
            hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby).account(creator).build());

            assertThatThrownBy(() -> {
                hobbyManagerRepository.saveAndFlush(HobbyManager.builder()
                        .hobby(hobby).account(creator).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("HobbyMember")
    class HobbyMemberTests {

        @Test
        @DisplayName("saves HobbyMember with audit columns")
        void savesWithAudit() {
            HobbyMember hm = hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby)
                    .account(user)
                                        .build());

            assertThat(hm.getId()).isNotNull();
            assertThat(hm.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects duplicate hobby-account pair")
        void rejectsDuplicate() {
            hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby).account(user).build());

            assertThatThrownBy(() -> {
                hobbyMemberRepository.saveAndFlush(HobbyMember.builder()
                        .hobby(hobby).account(user).build());
            }).isInstanceOf(Exception.class);
        }
    }
}
