package com.lume.infrastructure.web;

import com.lume.infrastructure.persistence.entity.LoginRateLimitBucketJpaEntity;
import com.lume.infrastructure.persistence.repository.LoginRateLimitBucketJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginRateLimitService {

    private static final int EVICTION_CHECK_INTERVAL = 100;

    private final LoginRateLimitBucketJpaRepository repository;
    private final Clock clock;

    private final AtomicInteger requestCounter = new AtomicInteger();

    public LoginRateLimitService(LoginRateLimitBucketJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public LoginRateLimitDecision registerAttempt(String clientIp, String identity, int maxAttempts, int windowSeconds) {
        LocalDateTime now = LocalDateTime.now(clock);
        String bucketKey = bucketKey(clientIp, identity);
        LoginRateLimitBucketJpaEntity bucket = repository.findLockedByBucketKey(bucketKey)
                .orElseGet(() -> {
                    LoginRateLimitBucketJpaEntity entity = new LoginRateLimitBucketJpaEntity();
                    entity.setBucketKey(bucketKey);
                    entity.setWindowStartedAt(now);
                    entity.setAttemptCount(0);
                    return entity;
                });

        if (bucket.getWindowStartedAt() == null || bucket.getWindowStartedAt().plusSeconds(windowSeconds).isBefore(now)) {
            bucket.setWindowStartedAt(now);
            bucket.setAttemptCount(0);
        }

        bucket.setAttemptCount(bucket.getAttemptCount() + 1);
        repository.save(bucket);

        if (requestCounter.incrementAndGet() % EVICTION_CHECK_INTERVAL == 0) {
            repository.deleteByUpdatedAtBefore(now.minusSeconds(windowSeconds * 2L));
        }

        int remaining = Math.max(0, maxAttempts - bucket.getAttemptCount());
        long retryAfterSeconds = Math.max(0, java.time.Duration.between(now, bucket.getWindowStartedAt().plusSeconds(windowSeconds)).toSeconds());
        boolean blocked = bucket.getAttemptCount() > maxAttempts;
        return new LoginRateLimitDecision(blocked, Math.max(0, remaining), retryAfterSeconds, maxAttempts);
    }

    private String bucketKey(String clientIp, String identity) {
        String normalizedIp = clientIp == null || clientIp.isBlank() ? "unknown-ip" : clientIp.trim().toLowerCase(Locale.ROOT);
        String normalizedIdentity = identity == null || identity.isBlank() ? "anonymous" : identity.trim().toLowerCase(Locale.ROOT);
        return normalizedIp + "|" + normalizedIdentity;
    }

    public record LoginRateLimitDecision(
            boolean blocked,
            int remainingAttempts,
            long retryAfterSeconds,
            int limit
    ) {
    }
}
