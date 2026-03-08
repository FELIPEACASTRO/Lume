package com.lume.workspace.inference.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AiMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public AiMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLatency(String provider, String model, String operation, String status, long latencyMs) {
        Timer.builder("lume.ai.request.latency")
                .tags(tags(provider, model, operation, status))
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(latencyMs, 0)));
    }

    public void incrementSuccess(String provider, String model, String operation) {
        counter("lume.ai.request.success", provider, model, operation, "completed").increment();
    }

    public void incrementError(String provider, String model, String operation, String status) {
        counter("lume.ai.request.error", provider, model, operation, status).increment();
    }

    public void incrementTimeout(String provider, String model, String operation) {
        counter("lume.ai.request.timeout", provider, model, operation, "timeout").increment();
    }

    public void incrementFallback(String provider, String model, String operation) {
        counter("lume.ai.request.fallback", provider, model, operation, "fallback").increment();
    }

    public void recordEstimatedCost(String provider, String model, String operation, Double costUsd) {
        if (costUsd == null) {
            return;
        }
        DistributionSummary.builder("lume.ai.request.estimated_cost_usd")
                .baseUnit("usd")
                .tags(tags(provider, model, operation, "estimated"))
                .register(meterRegistry)
                .record(costUsd);
    }

    private Counter counter(String metric, String provider, String model, String operation, String status) {
        return Counter.builder(metric)
                .tags(tags(provider, model, operation, status))
                .register(meterRegistry);
    }

    private Tags tags(String provider, String model, String operation, String status) {
        return Tags.of(
                "provider", provider == null ? "unknown" : provider,
                "model", model == null ? "unknown" : model,
                "operation", operation == null ? "unknown" : operation,
                "status", status == null ? "unknown" : status
        );
    }
}
