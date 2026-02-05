package com.jokahobby.modules.account;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MockMvcTest
class AccountTagRepositoryTest extends AbstractContainerBaseTest {

    @Autowired AccountTagRepository accountTagRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TagRepository tagRepository;

    private Account account;
    private Tag tag;

    @BeforeEach
    void setUp() {
        accountTagRepository.deleteAll();
        account = accountRepository.save(Account.builder()
                .email("test@example.com")
                .nickname("tester")
                .provider("google")
                .providerId("google-1")
                .joinedAt(LocalDateTime.now())
                .build());
        tag = tagRepository.save(Tag.builder().title("spring").build());
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists AccountTag with audit columns")
        void savesWithAudit() {
            AccountTag accountTag = accountTagRepository.save(AccountTag.builder()
                    .account(account)
                    .tag(tag)
                                        .build());

            assertThat(accountTag.getId()).isNotNull();
            assertThat(accountTag.getCreatedAt()).isNotNull();
            assertThat(accountTag.getAccount().getId()).isEqualTo(account.getId());
            assertThat(accountTag.getTag().getId()).isEqualTo(tag.getId());
        }

        @Test
        @DisplayName("rejects duplicate account-tag pair")
        void rejectsDuplicate() {
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag).build());

            assertThatThrownBy(() -> {
                accountTagRepository.saveAndFlush(AccountTag.builder()
                        .account(account).tag(tag).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByAccountId")
    class FindAllByAccountId {

        @Test
        @DisplayName("returns all tags for account")
        void returnsAll() {
            Tag tag2 = tagRepository.save(Tag.builder().title("java").build());
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag).build());
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag2).build());

            List<AccountTag> result = accountTagRepository.findAllByAccountId(account.getId());

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list for account without tags")
        void returnsEmpty() {
            List<AccountTag> result = accountTagRepository.findAllByAccountId(account.getId());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByAccountAndTag")
    class FindByAccountAndTag {

        @Test
        @DisplayName("returns matching AccountTag")
        void returnsMatch() {
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag).build());

            assertThat(accountTagRepository.findByAccountAndTag(account, tag)).isPresent();
        }

        @Test
        @DisplayName("returns empty for non-existing pair")
        void returnsEmpty() {
            assertThat(accountTagRepository.findByAccountAndTag(account, tag)).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByAccountAndTag")
    class DeleteByAccountAndTag {

        @Test
        @DisplayName("deletes the matching row")
        void deletes() {
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag).build());

            accountTagRepository.deleteByAccountAndTag(account, tag);

            assertThat(accountTagRepository.findByAccountAndTag(account, tag)).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByAccountAndTag")
    class ExistsByAccountAndTag {

        @Test
        @DisplayName("returns true when exists")
        void returnsTrue() {
            accountTagRepository.save(AccountTag.builder()
                    .account(account).tag(tag).build());

            assertThat(accountTagRepository.existsByAccountAndTag(account, tag)).isTrue();
        }

        @Test
        @DisplayName("returns false when not exists")
        void returnsFalse() {
            assertThat(accountTagRepository.existsByAccountAndTag(account, tag)).isFalse();
        }
    }
}
