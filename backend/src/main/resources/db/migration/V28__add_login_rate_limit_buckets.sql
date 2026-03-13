CREATE TABLE IF NOT EXISTS login_rate_limit_buckets (
    bucket_key         VARCHAR(255) PRIMARY KEY,
    window_started_at  TIMESTAMP   NOT NULL,
    attempt_count      INTEGER     NOT NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_login_rate_limit_buckets_updated_at
    ON login_rate_limit_buckets (updated_at);
