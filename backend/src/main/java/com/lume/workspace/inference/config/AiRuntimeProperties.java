package com.lume.workspace.inference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "lume.ai")
public class AiRuntimeProperties {

    private ProviderRuntimeProperties defaults = new ProviderRuntimeProperties();
    private Map<String, ProviderRuntimeProperties> providers = new HashMap<>();

    public ProviderRuntimeProperties getDefaults() {
        return defaults;
    }

    public void setDefaults(ProviderRuntimeProperties defaults) {
        this.defaults = defaults;
    }

    public Map<String, ProviderRuntimeProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderRuntimeProperties> providers) {
        this.providers = providers;
    }

    public ProviderRuntimeProperties forProvider(String providerCode) {
        ProviderRuntimeProperties merged = new ProviderRuntimeProperties();
        merged.applyFrom(defaults);
        ProviderRuntimeProperties specific = providers.get(providerCode);
        if (specific != null) {
            merged.applyFrom(specific);
        }
        return merged;
    }

    public static class ProviderRuntimeProperties {
        private long connectTimeoutMs = 4_000;
        private long readTimeoutMs = 30_000;
        private RetryProperties retry = new RetryProperties();
        private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
        private BulkheadProperties bulkhead = new BulkheadProperties();
        private RateLimiterProperties rateLimiter = new RateLimiterProperties();
        private PricingProperties pricing = new PricingProperties();

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public long getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(long readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }

        public CircuitBreakerProperties getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreakerProperties circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public BulkheadProperties getBulkhead() {
            return bulkhead;
        }

        public void setBulkhead(BulkheadProperties bulkhead) {
            this.bulkhead = bulkhead;
        }

        public RateLimiterProperties getRateLimiter() {
            return rateLimiter;
        }

        public void setRateLimiter(RateLimiterProperties rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        public PricingProperties getPricing() {
            return pricing;
        }

        public void setPricing(PricingProperties pricing) {
            this.pricing = pricing;
        }

        private void applyFrom(ProviderRuntimeProperties source) {
            if (source == null) {
                return;
            }
            this.connectTimeoutMs = source.connectTimeoutMs;
            this.readTimeoutMs = source.readTimeoutMs;
            this.retry = source.retry.copy();
            this.circuitBreaker = source.circuitBreaker.copy();
            this.bulkhead = source.bulkhead.copy();
            this.rateLimiter = source.rateLimiter.copy();
            this.pricing = source.pricing.copy();
        }
    }

    public static class RetryProperties {
        private int maxAttempts = 2;
        private long initialBackoffMs = 250;
        private long maxBackoffMs = 1_500;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMs() {
            return initialBackoffMs;
        }

        public void setInitialBackoffMs(long initialBackoffMs) {
            this.initialBackoffMs = initialBackoffMs;
        }

        public long getMaxBackoffMs() {
            return maxBackoffMs;
        }

        public void setMaxBackoffMs(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
        }

        private RetryProperties copy() {
            RetryProperties copy = new RetryProperties();
            copy.maxAttempts = maxAttempts;
            copy.initialBackoffMs = initialBackoffMs;
            copy.maxBackoffMs = maxBackoffMs;
            return copy;
        }
    }

    public static class CircuitBreakerProperties {
        private int failureRateThreshold = 50;
        private int slidingWindowSize = 6;
        private long openStateDurationMs = 15_000;

        public int getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public long getOpenStateDurationMs() {
            return openStateDurationMs;
        }

        public void setOpenStateDurationMs(long openStateDurationMs) {
            this.openStateDurationMs = openStateDurationMs;
        }

        private CircuitBreakerProperties copy() {
            CircuitBreakerProperties copy = new CircuitBreakerProperties();
            copy.failureRateThreshold = failureRateThreshold;
            copy.slidingWindowSize = slidingWindowSize;
            copy.openStateDurationMs = openStateDurationMs;
            return copy;
        }
    }

    public static class BulkheadProperties {
        private int maxConcurrentCalls = 8;

        public int getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(int maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }

        private BulkheadProperties copy() {
            BulkheadProperties copy = new BulkheadProperties();
            copy.maxConcurrentCalls = maxConcurrentCalls;
            return copy;
        }
    }

    public static class RateLimiterProperties {
        private int permitsPerMinute = 120;

        public int getPermitsPerMinute() {
            return permitsPerMinute;
        }

        public void setPermitsPerMinute(int permitsPerMinute) {
            this.permitsPerMinute = permitsPerMinute;
        }

        private RateLimiterProperties copy() {
            RateLimiterProperties copy = new RateLimiterProperties();
            copy.permitsPerMinute = permitsPerMinute;
            return copy;
        }
    }

    public static class PricingProperties {
        private Double inputPer1kUsd;
        private Double outputPer1kUsd;

        public Double getInputPer1kUsd() {
            return inputPer1kUsd;
        }

        public void setInputPer1kUsd(Double inputPer1kUsd) {
            this.inputPer1kUsd = inputPer1kUsd;
        }

        public Double getOutputPer1kUsd() {
            return outputPer1kUsd;
        }

        public void setOutputPer1kUsd(Double outputPer1kUsd) {
            this.outputPer1kUsd = outputPer1kUsd;
        }

        private PricingProperties copy() {
            PricingProperties copy = new PricingProperties();
            copy.inputPer1kUsd = inputPer1kUsd;
            copy.outputPer1kUsd = outputPer1kUsd;
            return copy;
        }
    }
}
