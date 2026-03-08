package com.lume.workspace.inference.orchestration;

import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.error.AiProviderException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
public class AiBulkheadRegistry {

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public void acquire(String providerCode, AiRuntimeProperties.ProviderRuntimeProperties settings) {
        State state = states.compute(providerCode, (ignored, existing) -> State.reconfigured(existing, settings.getBulkhead().getMaxConcurrentCalls()));
        if (!state.semaphore().tryAcquire()) {
            throw new AiProviderException("Bulkhead saturado para " + providerCode + ".", true);
        }
    }

    public void release(String providerCode) {
        State state = states.get(providerCode);
        if (state != null) {
            state.semaphore().release();
        }
    }

    private record State(int maxConcurrentCalls, Semaphore semaphore) {
        private static State reconfigured(State current, int configuredMaxConcurrentCalls) {
            int target = Math.max(1, configuredMaxConcurrentCalls);
            if (current == null || current.maxConcurrentCalls != target) {
                return new State(target, new Semaphore(target));
            }
            return current;
        }
    }
}
