CREATE TABLE support_tickets (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    created_by_user_id BIGINT,
    category VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    resolution_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_support_tickets_workspace_updated_at
    ON support_tickets (workspace_id, updated_at);

CREATE TABLE workspace_byok_connections (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    provider_code VARCHAR(80) NOT NULL,
    connection_name VARCHAR(160) NOT NULL,
    secret_ref VARCHAR(160) NOT NULL,
    scope_label VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    health_status VARCHAR(32) NOT NULL,
    last_validated_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_workspace_byok_connection_name
    ON workspace_byok_connections (workspace_id, connection_name);

CREATE INDEX idx_workspace_byok_connections_workspace_provider
    ON workspace_byok_connections (workspace_id, provider_code);
