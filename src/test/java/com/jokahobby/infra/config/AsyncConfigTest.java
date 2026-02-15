package com.jokahobby.infra.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AsyncConfigTest.TestConfig.class)
class AsyncConfigTest {

    @Configuration
    @EnableAsync
    static class TestConfig implements AsyncConfigurer {

        @Override
        public Executor getAsyncExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(5);
            executor.setMaxPoolSize(5);
            executor.setQueueCapacity(50);
            executor.setThreadNamePrefix("Async-");
            executor.initialize();
            return executor;
        }

        @Bean
        public AsyncTestListener asyncTestListener() {
            return new AsyncTestListener();
        }
    }

    record TestEvent(int index, CountDownLatch latch) {}

    static class AsyncTestListener {

        private final List<String> capturedThreadNames = new CopyOnWriteArrayList<>();

        @Async
        @EventListener
        public void handle(TestEvent event) throws InterruptedException {
            String threadName = Thread.currentThread().getName();
            capturedThreadNames.add(threadName);

            System.out.printf("[ASYNC] Event #%d → Thread: %s%n", event.index(), threadName);

            Thread.sleep(500);
            event.latch().countDown();
        }

        public List<String> getCapturedThreadNames() {
            return capturedThreadNames;
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AsyncTestListener asyncTestListener;

    @Test
    @DisplayName("5 events run concurrently on 5 separate Async- threads")
    void fiveEvents_runOnFiveSeparateAsyncThreads() throws Exception {
        int eventCount = 5;
        CountDownLatch latch = new CountDownLatch(eventCount);
        String callerThread = Thread.currentThread().getName();

        System.out.println();
        System.out.println("========================================");
        System.out.println("[CALLER] Test thread: " + callerThread);
        System.out.println("[START]  Publishing " + eventCount + " events...");
        System.out.println("========================================");

        for (int i = 1; i <= eventCount; i++) {
            eventPublisher.publishEvent(new TestEvent(i, latch));
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        List<String> threadNames = asyncTestListener.getCapturedThreadNames();

        System.out.println("========================================");
        System.out.println("[RESULT] All threads used:");
        threadNames.forEach(name -> System.out.println("         → " + name));
        long distinctCount = threadNames.stream().distinct().count();
        System.out.println("[RESULT] Distinct thread count: " + distinctCount);
        System.out.println("========================================");
        System.out.println();

        assertThat(threadNames).hasSize(eventCount);
        assertThat(threadNames).allMatch(name -> name.startsWith("Async-"));
        assertThat(threadNames).noneMatch(name -> name.equals(callerThread));
        assertThat(distinctCount).isEqualTo(eventCount);
    }
}
