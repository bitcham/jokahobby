package com.jokahobby.modules.hobby;

import com.jokahobby.api.service.HobbyApplicationService;
import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HobbyMemberConcurrencyTest extends AbstractContainerBaseTest {

    @Autowired HobbyApplicationService hobbyApplicationService;
    @Autowired AccountRepository accountRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyMemberRepository hobbyMemberRepository;

    private Account managerAccount;
    private Hobby hobby;

    @BeforeEach
    void setUp() {
        hobbyMemberRepository.deleteAllInBatch();
        hobbyManagerRepository.deleteAllInBatch();
        hobbyRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();

        managerAccount = accountRepository.save(Account.builder()
                .email("manager@test.com")
                .nickname("manager-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-manager-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());

        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby-" + UUID.randomUUID().toString().substring(0, 8))
                .title("Test Hobby " + UUID.randomUUID().toString().substring(0, 8))
                .shortDescription("desc")
                .fullDescription("full desc")
                .published(true)
                .publishedDateTime(Instant.now())
                .recruiting(true)
                .memberCount(1)
                .build());

        hobbyManagerRepository.save(HobbyManager.builder()
                .hobby(hobby).account(managerAccount).build());
    }

    @AfterEach
    void tearDown() {
        hobbyMemberRepository.deleteAllInBatch();
        hobbyManagerRepository.deleteAllInBatch();
        hobbyRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("10 concurrent joins: memberCount == 11 (1 manager + 10 members)")
    void concurrentJoins_memberCountCorrect() throws InterruptedException {
        int threadCount = 10;
        List<Account> accounts = createAccounts(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (Account account : accounts) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    hobbyApplicationService.joinHobby(hobby.getPath(), account);
                } catch (Exception ignored) {
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        Hobby updated = hobbyRepository.findById(hobby.getId()).orElseThrow();
        assertThat(updated.getMemberCount()).isEqualTo(11);

        long memberRows = hobbyMemberRepository.findAllByHobbyId(hobby.getId()).size();
        assertThat(memberRows).isEqualTo(10);
    }

    @Test
    @DisplayName("concurrent join + leave: memberCount stays consistent")
    void concurrentJoinAndLeave_memberCountConsistent() throws InterruptedException {
        // Add existing member via application service (committed in its own transaction)
        Account existingMember = accountRepository.save(Account.builder()
                .email("existing@test.com")
                .nickname("existing-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-existing-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());
        hobbyApplicationService.joinHobby(hobby.getPath(), existingMember);
        // Now: manager(1) + existing member(1) = memberCount 2

        int joinCount = 5;
        List<Account> joinAccounts = createAccounts(joinCount);

        int totalThreads = joinCount + 1; // 5 joins + 1 leave
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        // 5 join threads
        for (Account account : joinAccounts) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    hobbyApplicationService.joinHobby(hobby.getPath(), account);
                } catch (Exception ignored) {
                }
            }));
        }

        // 1 leave thread
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                hobbyApplicationService.leaveHobby(hobby.getPath(), existingMember);
            } catch (Exception ignored) {
            }
        }));

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        Hobby updated = hobbyRepository.findById(hobby.getId()).orElseThrow();
        // 2 (initial) + 5 (joins) - 1 (leave) = 6
        assertThat(updated.getMemberCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("same account 10 concurrent joins: memberCount == 2 (1 manager + 1 member)")
    void concurrentDuplicateJoins_memberCountCorrect() throws InterruptedException {
        Account singleAccount = accountRepository.save(Account.builder()
                .email("dup@test.com")
                .nickname("dup-" + UUID.randomUUID().toString().substring(0, 8))
                .provider("google")
                .providerId("google-dup-" + UUID.randomUUID())
                .joinedAt(Instant.now())
                .build());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    hobbyApplicationService.joinHobby(hobby.getPath(), singleAccount);
                } catch (Exception ignored) {
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> future : futures) {
            try { future.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();

        Hobby updated = hobbyRepository.findById(hobby.getId()).orElseThrow();
        assertThat(updated.getMemberCount()).isEqualTo(2);

        long memberRows = hobbyMemberRepository.findAllByHobbyId(hobby.getId()).size();
        assertThat(memberRows).isEqualTo(1);
    }

    private List<Account> createAccounts(int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            accounts.add(accountRepository.save(Account.builder()
                    .email("user" + i + "@test.com")
                    .nickname("user" + i + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .provider("google")
                    .providerId("google-user" + i + "-" + UUID.randomUUID())
                    .joinedAt(Instant.now())
                    .build()));
        }
        return accounts;
    }
}
