package com.yichi.onepiece.assistant.agent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Per-user guard for the assistant: one answer in flight at a time and at most
 * {@value #MAX_REQUESTS_PER_WINDOW} questions a minute. In-memory, which is
 * enough for a single backend instance.
 */
@Component
public class AssistantRateLimiter {
    static final int MAX_REQUESTS_PER_WINDOW = 10;
    static final Duration WINDOW = Duration.ofMinutes(1);

    public enum Decision { ACQUIRED, BUSY, RATE_LIMITED }

    private final Clock clock;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Deque<Instant>> recentRequests = new ConcurrentHashMap<>();

    public AssistantRateLimiter(@Qualifier("onepieceClock") Clock clock) {
        this.clock = clock;
    }

    /** On {@link Decision#ACQUIRED} the caller must {@link #release} when the answer ends. */
    public Decision tryAcquire(UUID userId) {
        Deque<Instant> requests = recentRequests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (requests) {
            if (inFlight.contains(userId)) {
                return Decision.BUSY;
            }
            Instant now = clock.instant();
            Instant windowStart = now.minus(WINDOW);
            while (!requests.isEmpty() && !requests.peekFirst().isAfter(windowStart)) {
                requests.pollFirst();
            }
            if (requests.size() >= MAX_REQUESTS_PER_WINDOW) {
                return Decision.RATE_LIMITED;
            }
            requests.addLast(now);
            inFlight.add(userId);
            return Decision.ACQUIRED;
        }
    }

    public void release(UUID userId) {
        inFlight.remove(userId);
    }
}
