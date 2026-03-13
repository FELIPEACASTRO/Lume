CREATE TABLE workspace_compliance_settings (
    workspace_id BIGINT PRIMARY KEY,
    retention_policy_status VARCHAR(32) NOT NULL,
    retention_days INTEGER,
    access_review_status VARCHAR(32) NOT NULL,
    access_review_frequency_days INTEGER,
    consent_tracking_enabled BOOLEAN NOT NULL,
    terms_version VARCHAR(64),
    updated_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_workspace_compliance_settings_updated_at
    ON workspace_compliance_settings (updated_at);
