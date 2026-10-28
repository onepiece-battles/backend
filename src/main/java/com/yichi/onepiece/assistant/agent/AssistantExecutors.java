package com.yichi.onepiece.assistant.agent;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * The assistant's bounded thread pools: one for answering questions (an SSE
 * stream each) and one for running a round's tool calls in parallel. Held
 * here rather than exposed as {@code Executor} beans so they never replace
 * Spring Boot's default task executor.
 */
@Component
public class AssistantExecutors implements DisposableBean {
    private final ThreadPoolTaskExecutor turns = pool("assistant-", 2, 8, 16);
    private final ThreadPoolTaskExecutor tools = pool("assistant-tool-", 4, 8, 32);

    public Executor turns() {
        return turns;
    }

    public Executor tools() {
        return tools;
    }

    @Override
    public void destroy() {
        turns.shutdown();
        tools.shutdown();
    }

    private static ThreadPoolTaskExecutor pool(String threadNamePrefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.initialize();
        return executor;
    }
}
