package com.lume.workspace.inference.orchestration;

import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.error.AiRateLimitException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRateLimiterRegistry {

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public void beforeCall(String providerCode, AiRuntimeProperties.ProviderRuntimeProperties settings) {
        State state = states.computeIfAbsent(providerCode, ignored -> new State());
        state.beforeCall(settings);
    }

    private static final class State {
        private int permitsPerMinute;
        private int usedPermits;
        private long windowStartedAtEpochMilli;

        private synchronized void beforeCall(AiRuntimeProperties.ProviderRuntimeProperties settings) {
            permitsPerMinute = Math.max(1, settings.getRateLimiter().getPermitsPerMinute());
            long now = Instant.now().toEpochMilli();
            if (windowStartedAtEpochMilli == 0 || now - windowStartedAtEpochMilli >= 60_000) {
                windowStartedAtEpochMilli = now;
                usedPermits = 0;
            }
            if (usedPermits >= permitsPerMinute) {
                throw new AiRateLimitException("Rate limiter local acionado.");
            }
            usedPermits++;
        }
    }
}
