package com.yichi.onepiece.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.assistant.agent.AssistantRateLimiter.Decision;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.testng.annotations.Test;

public class AssistantRateLimiterTests {
    private final UUID user = UUID.randomUUID();

    @Test
    public void allowsOneAnswerInFlightPerUser() {
        var limiter = new AssistantRateLimiter(new MutableClock());

        assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.ACQUIRED);
        assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.BUSY);
        assertThat(limiter.tryAcquire(UUID.randomUUID())).isEqualTo(Decision.ACQUIRED);

        limiter.release(user);

        assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.ACQUIRED);
    }

    @Test
    public void limitsQuestionsPerMinuteAndRecoversAfterTheWindow() {
        var clock = new MutableClock();
        var limiter = new AssistantRateLimiter(clock);
        for (int i = 0; i < AssistantRateLimiter.MAX_REQUESTS_PER_WINDOW; i++) {
            assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.ACQUIRED);
            limiter.release(user);
        }

        assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.RATE_LIMITED);

        clock.advance(AssistantRateLimiter.WINDOW.plusSeconds(1));

        assertThat(limiter.tryAcquire(user)).isEqualTo(Decision.ACQUIRED);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-28T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
