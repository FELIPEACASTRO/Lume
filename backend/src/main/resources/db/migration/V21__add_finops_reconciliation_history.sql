CREATE TABLE workspace_finops_reconciliation_runs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    run_mode VARCHAR(32) NOT NULL,
    reconciliation_status VARCHAR(64) NOT NULL,
    subscription_credits INTEGER NOT NULL,
    ledger_credit_balance INTEGER NOT NULL,
    credit_drift INTEGER NOT NULL,
    invoices_total BIGINT NOT NULL,
    invoices_paid BIGINT NOT NULL,
    invoices_paid_amount_brl NUMERIC(12, 2) NOT NULL,
    payment_events_total BIGINT NOT NULL,
    payment_events_processed BIGINT NOT NULL,
    payment_events_processed_amount_brl NUMERIC(12, 2) NOT NULL,
    orphan_payment_events BIGINT NOT NULL,
    pending_payment_events BIGINT NOT NULL,
    credit_fix_applied BOOLEAN NOT NULL,
    credit_fix_delta INTEGER,
    recommendation VARCHAR(255),
    executed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_workspace_finops_reconciliation_runs_workspace_executed_at
    ON workspace_finops_reconciliation_runs (workspace_id, executed_at);
