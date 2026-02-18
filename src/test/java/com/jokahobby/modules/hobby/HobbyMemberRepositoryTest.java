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
class HobbyMemberRepositoryTest extends AbstractContainerBaseTest {

    @Autowired HobbyMemberRepository hobbyMemberRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired AccountRepository accountRepository;

    private Hobby hobby;
    private Account member;

    @BeforeEach
    void setUp() {
        hobbyMemberRepository.deleteAll();
        member = accountRepository.save(Account.builder()
                .email("member@example.com")
                .nickname("member")
                .provider("google")
                .providerId("google-mem")
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
        @DisplayName("persists HobbyMember with audit columns")
        void savesWithAudit() {
            HobbyMember hobbyMember = hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby)
                    .account(member)
                    .build());

            assertThat(hobbyMember.getId()).isNotNull();
            assertThat(hobbyMember.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects duplicate hobby-account pair")
        void rejectsDuplicate() {
            hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby).account(member).build());

            assertThatThrownBy(() -> {
                hobbyMemberRepository.saveAndFlush(HobbyMember.builder()
                        .hobby(hobby).account(member).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByHobbyId")
    class FindAllByHobbyId {

        @Test
        @DisplayName("returns all members for hobby with fetched account data")
        void returnsAllWithFetchedData() {
            Account member2 = accountRepository.save(Account.builder()
                    .email("member2@example.com")
                    .nickname("member2")
                    .provider("google")
                    .providerId("google-mem2")
                    .joinedAt(Instant.now())
                    .build());
            hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby).account(member).build());
            hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby).account(member2).build());

            List<HobbyMember> result = hobbyMemberRepository.findAllByHobbyId(hobby.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccount().getNickname()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deleteByHobbyAndAccount")
    class DeleteByHobbyAndAccount {

        @Test
        @DisplayName("deletes the matching row")
        void deletes() {
            hobbyMemberRepository.save(HobbyMember.builder()
                    .hobby(hobby).account(member).build());

            hobbyMemberRepository.deleteByHobbyAndAccount(hobby, member);

            assertThat(hobbyMemberRepository.existsByHobbyAndAccount(hobby, member)).isFalse();
        }
    }
}
