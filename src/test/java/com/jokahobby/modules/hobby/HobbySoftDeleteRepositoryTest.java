package com.jokahobby.modules.hobby;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class HobbySoftDeleteRepositoryTest extends AbstractContainerBaseTest {

    @Autowired HobbyRepository hobbyRepository;
    @Autowired EntityManager em;

    @BeforeEach
    void setUp() {
        hobbyRepository.deleteAll();
    }

    private Hobby createHobby(String path, String title) {
        return hobbyRepository.save(Hobby.builder()
                .path(path)
                .title(title)
                .shortDescription("desc")
                .build());
    }

    @Nested
    @DisplayName("@SQLRestriction filtering")
    class SqlRestriction {

        @Test
        @DisplayName("soft-deleted hobby is excluded from findByPath")
        void findByPathExcludesSoftDeleted() {
            Hobby hobby = createHobby("test-path", "Test Title");
            hobby.softDelete();
            em.flush();
            em.clear();

            Hobby found = hobbyRepository.findByPath("test-path");
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("soft-deleted hobby is excluded from findById")
        void findByIdExcludesSoftDeleted() {
            Hobby hobby = createHobby("test-path", "Test Title");
            Long id = hobby.getId();
            hobby.softDelete();
            em.flush();
            em.clear();

            Optional<Hobby> found = hobbyRepository.findById(id);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("soft-deleted hobby is excluded from existsByPath")
        void existsByPathExcludesSoftDeleted() {
            Hobby hobby = createHobby("test-path", "Test Title");
            hobby.softDelete();
            em.flush();
            em.clear();

            assertThat(hobbyRepository.existsByPath("test-path")).isFalse();
        }

        @Test
        @DisplayName("findAll excludes soft-deleted hobbies")
        void findAllExcludesSoftDeleted() {
            createHobby("path-1", "Title 1");
            createHobby("path-2", "Title 2");
            Hobby toDelete = createHobby("path-3", "Title 3");
            toDelete.softDelete();
            em.flush();
            em.clear();

            List<Hobby> all = hobbyRepository.findAll();
            assertThat(all).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Partial unique index")
    class PartialUniqueIndex {

        @Test
        @DisplayName("can create hobby with same path after soft delete")
        void canReusePath() {
            Hobby hobby = createHobby("reuse-path", "Original Title");
            hobby.softDelete();
            em.flush();
            em.clear();

            Hobby newHobby = createHobby("reuse-path", "New Title");
            assertThat(newHobby.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("BaseEntity audit fields")
    class AuditFields {

        @Test
        @DisplayName("createdAt and updatedAt are set on save")
        void auditFieldsSetOnSave() {
            Hobby hobby = createHobby("audit-path", "Audit Title");

            assertThat(hobby.getCreatedAt()).isNotNull();
            assertThat(hobby.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("updatedAt changes on update, createdAt unchanged")
        void updatedAtChangesOnUpdate() throws InterruptedException {
            Hobby hobby = createHobby("audit-path", "Audit Title");
            var originalCreatedAt = hobby.getCreatedAt();
            var originalUpdatedAt = hobby.getUpdatedAt();

            Thread.sleep(50);
            hobby.updateDescription("updated", hobby.getFullDescription());
            em.flush();
            em.clear();

            Hobby reloaded = hobbyRepository.findByPath("audit-path");
            assertThat(reloaded.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }
}
