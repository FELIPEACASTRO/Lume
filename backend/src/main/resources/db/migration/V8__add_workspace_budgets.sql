CREATE TABLE IF NOT EXISTS workspace_budgets (
    id                 BIGSERIAL     PRIMARY KEY,
    workspace_id       BIGINT        NOT NULL UNIQUE,
    cost_center        VARCHAR(120)  NOT NULL DEFAULT 'core_now',
    chargeback_mode    VARCHAR(32)   NOT NULL DEFAULT 'showback',
    soft_limit_credits INTEGER       NOT NULL DEFAULT 300,
    hard_limit_credits INTEGER       NOT NULL DEFAULT 450,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspace_budgets_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_workspace_budgets_workspace
    ON workspace_budgets (workspace_id);
