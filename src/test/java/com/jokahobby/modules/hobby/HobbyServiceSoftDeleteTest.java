package com.jokahobby.modules.hobby;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MockMvcTest
class HobbyServiceSoftDeleteTest extends AbstractContainerBaseTest {

    @Autowired HobbyService hobbyService;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired EntityManager em;

    private Account manager;

    @BeforeEach
    void setUp() {
        hobbyRepository.deleteAll();
        manager = accountRepository.save(Account.builder()
                .email("manager@test.com")
                .nickname("manager")
                .provider("google")
                .providerId("google-mgr")
                .joinedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("remove() soft-deletes hobby via dirty checking")
    void removeSoftDeletes() {
        Hobby hobby = hobbyRepository.save(Hobby.builder()
                .path("removable")
                .title("Removable Hobby")
                .shortDescription("desc")
                .published(false)
                .build());

        hobbyService.remove(hobby);
        em.flush();
        em.clear();

        assertThat(hobbyRepository.findByPath("removable")).isEmpty();
    }

    @Test
    @DisplayName("remove() throws BusinessException for non-removable hobby")
    void removeNonRemovableThrows() {
        Hobby hobby = hobbyRepository.save(Hobby.builder()
                .path("not-removable")
                .title("Not Removable")
                .shortDescription("desc")
                .published(true)
                .closed(false)
                .build());

        assertThatThrownBy(() -> hobbyService.remove(hobby))
                .isInstanceOf(BusinessException.class);
    }
}
