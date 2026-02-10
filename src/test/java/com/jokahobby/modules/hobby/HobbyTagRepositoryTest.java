package com.jokahobby.modules.hobby;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagRepository;
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
class HobbyTagRepositoryTest extends AbstractContainerBaseTest {

    @Autowired HobbyTagRepository hobbyTagRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired TagRepository tagRepository;
    @Autowired AccountRepository accountRepository;

    private Hobby hobby;
    private Tag tag;
    private Account manager;

    @BeforeEach
    void setUp() {
        hobbyTagRepository.deleteAll();
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
        tag = tagRepository.save(Tag.builder().title("spring").build());
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists HobbyTag with audit columns")
        void savesWithAudit() {
            HobbyTag hobbyTag = hobbyTagRepository.save(HobbyTag.builder()
                    .hobby(hobby)
                    .tag(tag)
                                        .build());

            assertThat(hobbyTag.getId()).isNotNull();
            assertThat(hobbyTag.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects duplicate hobby-tag pair")
        void rejectsDuplicate() {
            hobbyTagRepository.save(HobbyTag.builder()
                    .hobby(hobby).tag(tag).build());

            assertThatThrownBy(() -> {
                hobbyTagRepository.saveAndFlush(HobbyTag.builder()
                        .hobby(hobby).tag(tag).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByHobbyId")
    class FindAllByHobbyId {

        @Test
        @DisplayName("returns all tags for hobby")
        void returnsAll() {
            Tag tag2 = tagRepository.save(Tag.builder().title("java").build());
            hobbyTagRepository.save(HobbyTag.builder()
                    .hobby(hobby).tag(tag).build());
            hobbyTagRepository.save(HobbyTag.builder()
                    .hobby(hobby).tag(tag2).build());

            List<HobbyTag> result = hobbyTagRepository.findAllByHobbyId(hobby.getId());

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deleteByHobbyAndTag")
    class DeleteByHobbyAndTag {

        @Test
        @DisplayName("deletes the matching row")
        void deletes() {
            hobbyTagRepository.save(HobbyTag.builder()
                    .hobby(hobby).tag(tag).build());

            hobbyTagRepository.deleteByHobbyAndTag(hobby, tag);

            assertThat(hobbyTagRepository.findByHobbyAndTag(hobby, tag)).isEmpty();
        }
    }
}
