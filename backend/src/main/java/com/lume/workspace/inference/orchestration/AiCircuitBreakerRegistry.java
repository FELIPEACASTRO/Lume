package com.lume.workspace.inference.orchestration;

import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.error.AiCircuitOpenException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiCircuitBreakerRegistry {

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public void beforeCall(String providerCode, AiRuntimeProperties.ProviderRuntimeProperties settings) {
        State state = states.computeIfAbsent(providerCode, key -> new State());
        if (state.isOpen()) {
            throw new AiCircuitOpenException("Circuit breaker aberto para " + providerCode + ".");
        }
        state.ensureConfig(settings);
    }

    public void recordSuccess(String providerCode, AiRuntimeProperties.ProviderRuntimeProperties settings) {
        State state = states.computeIfAbsent(providerCode, key -> new State());
        state.ensureConfig(settings);
        state.record(false);
    }

    public void recordFailure(String providerCode, AiRuntimeProperties.ProviderRuntimeProperties settings) {
        State state = states.computeIfAbsent(providerCode, key -> new State());
        state.ensureConfig(settings);
        state.record(true);
    }

    private static final class State {
        private final Deque<Boolean> failures = new ArrayDeque<>();
        private int failureRateThreshold;
        private int slidingWindowSize;
        private Duration openDuration = Duration.ofSeconds(15);
        private Instant openUntil = Instant.MIN;

        private void ensureConfig(AiRuntimeProperties.ProviderRuntimeProperties settings) {
            this.failureRateThreshold = settings.getCircuitBreaker().getFailureRateThreshold();
            this.slidingWindowSize = Math.max(1, settings.getCircuitBreaker().getSlidingWindowSize());
            this.openDuration = Duration.ofMillis(Math.max(1, settings.getCircuitBreaker().getOpenStateDurationMs()));
            while (failures.size() > slidingWindowSize) {
                failures.removeFirst();
            }
        }

        private boolean isOpen() {
            return Instant.now().isBefore(openUntil);
        }

        private void record(boolean failure) {
            if (failure) {
                failures.addLast(Boolean.TRUE);
            } else {
                failures.addLast(Boolean.FALSE);
                openUntil = Instant.MIN;
            }
            while (failures.size() > slidingWindowSize) {
                failures.removeFirst();
            }
            if (failures.size() < slidingWindowSize) {
                return;
            }
            long failureCount = failures.stream().filter(Boolean::booleanValue).count();
            int failureRate = (int) Math.round((failureCount * 100.0d) / failures.size());
            if (failureRate >= failureRateThreshold) {
                openUntil = Instant.now().plus(openDuration);
            }
        }
    }
}
