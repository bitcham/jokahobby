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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MockMvcTest
class HobbyManagerRepositoryTest extends AbstractContainerBaseTest {

    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired AccountRepository accountRepository;

    private Hobby hobby;
    private Account manager;
    private Account promoter;

    @BeforeEach
    void setUp() {
        hobbyManagerRepository.deleteAll();
        promoter = accountRepository.save(Account.builder()
                .email("promoter@example.com")
                .nickname("promoter")
                .provider("google")
                .providerId("google-promoter")
                .joinedAt(Instant.now())
                .build());
        manager = accountRepository.save(Account.builder()
                .email("manager@example.com")
                .nickname("manager")
                .provider("google")
                .providerId("google-mgr")
                .joinedAt(Instant.now())
                .build());
        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .build());
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists HobbyManager with audit columns")
        void savesWithAudit() {
            HobbyManager hobbyManager = hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby)
                    .account(manager)
                    .promotedBy(promoter)
                    .build());

            assertThat(hobbyManager.getId()).isNotNull();
            assertThat(hobbyManager.getCreatedAt()).isNotNull();
            assertThat(hobbyManager.getPromotedBy().getId()).isEqualTo(promoter.getId());
        }

        @Test
        @DisplayName("rejects duplicate hobby-account pair")
        void rejectsDuplicate() {
            hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby).account(manager).promotedBy(promoter).build());

            assertThatThrownBy(() -> {
                hobbyManagerRepository.saveAndFlush(HobbyManager.builder()
                        .hobby(hobby).account(manager).promotedBy(promoter).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByHobbyId")
    class FindAllByHobbyId {

        @Test
        @DisplayName("returns all managers for hobby with fetched account data")
        void returnsAllWithFetchedData() {
            Account manager2 = accountRepository.save(Account.builder()
                    .email("manager2@example.com")
                    .nickname("manager2")
                    .provider("google")
                    .providerId("google-mgr2")
                    .joinedAt(Instant.now())
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby).account(manager).promotedBy(promoter).build());
            hobbyManagerRepository.save(HobbyManager.builder()
                    .hobby(hobby).account(manager2).promotedBy(promoter).build());

            List<HobbyManager> result = hobbyManagerRepository.findAllByHobbyId(hobby.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccount().getNickname()).isNotNull();
        }
    }
}
