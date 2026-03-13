-- ============================================================================
-- V26: Missing FK constraints + performance indexes
--
-- Addresses:
--   C1  – FK constraints on V18/V19/V21 tables (workspace_id → workspaces)
--   C3  – memberships composite index for auth lookups
--   C4  – notifications unread badge index
--   C5  – usage events distinct-actor index
--   C6  – support tickets governance dashboard index
--   C7  – payment events status-based count index
--   C9  – user_preferences.active_workspace_id FK
--   C10 – workspace_cost_ledger_entries.workspace_id NOT NULL
-- ============================================================================

-- ── C1: Missing FK constraints on commercial/finops tables ──────────────────

ALTER TABLE workspace_onboarding_profiles
    ADD CONSTRAINT fk_onboarding_profiles_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_subscriptions
    ADD CONSTRAINT fk_subscriptions_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_usage_events
    ADD CONSTRAINT fk_usage_events_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_credit_ledger_entries
    ADD CONSTRAINT fk_credit_ledger_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_invoices
    ADD CONSTRAINT fk_invoices_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_finops_reconciliation_runs
    ADD CONSTRAINT fk_finops_reconciliation_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

-- C10: Make workspace_id NOT NULL on cost ledger (backfill orphans first)
DELETE FROM workspace_cost_ledger_entries WHERE workspace_id IS NULL;

ALTER TABLE workspace_cost_ledger_entries
    ALTER COLUMN workspace_id SET NOT NULL;

ALTER TABLE workspace_cost_ledger_entries
    ADD CONSTRAINT fk_cost_ledger_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

-- C9: FK on user_preferences.active_workspace_id
ALTER TABLE user_preferences
    ADD CONSTRAINT fk_user_preferences_active_workspace
    FOREIGN KEY (active_workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

-- ── C3: Membership auth/scoping indexes ─────────────────────────────────────
-- Covers: findByUserIdAndActiveTrueOrderByCreatedAtAsc
--         findByUserIdAndWorkspaceIdAndActiveTrue
CREATE INDEX IF NOT EXISTS idx_memberships_user_active_created
    ON memberships (user_id, active, created_at);

-- Covers: findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc
--         countByWorkspaceIdAndActiveTrue
CREATE INDEX IF NOT EXISTS idx_memberships_workspace_active_created
    ON memberships (workspace_id, active, created_at);

-- ── C4: Notification unread badge index ─────────────────────────────────────
-- Covers: countByWorkspaceIdAndReadFalse
--         findByWorkspaceIdAndReadFalseOrderByCreatedAtDesc
CREATE INDEX IF NOT EXISTS idx_notifications_workspace_read_created
    ON notifications (workspace_id, is_read, created_at DESC);

-- ── C5: Usage event distinct-actor index ────────────────────────────────────
-- Covers: countDistinctActorUserIdByWorkspaceIdAndActorUserIdIsNotNull...
CREATE INDEX IF NOT EXISTS idx_usage_events_workspace_actor_created
    ON workspace_usage_events (workspace_id, actor_user_id, created_at);

-- ── C6: Support ticket governance dashboard ─────────────────────────────────
-- Covers: countByWorkspaceIdAndStatusIn
--         countByWorkspaceIdAndStatusInAndSeverity
--         findByWorkspaceIdAndStatusIn (for SLA breach check)
CREATE INDEX IF NOT EXISTS idx_support_tickets_workspace_status_severity
    ON support_tickets (workspace_id, status, severity);

-- ── C7: Payment event status counts ─────────────────────────────────────────
-- Covers: countByWorkspaceIdAndStatus
--         countByWorkspaceIdAndStatusAndProcessedAtIsNull
--         sumAmountByWorkspaceIdAndStatus
CREATE INDEX IF NOT EXISTS idx_payment_events_workspace_status
    ON workspace_payment_events (workspace_id, status);

-- ── Additional: invoice status for billing queries ──────────────────────────
-- Covers: countByWorkspaceIdAndStatus, sumAmountByWorkspaceIdAndStatus
CREATE INDEX IF NOT EXISTS idx_invoices_workspace_status
    ON workspace_invoices (workspace_id, status);

-- ── Additional: cost ledger status for anomaly detection ────────────────────
-- Covers: countByWorkspaceIdAndStatus
--         countByWorkspaceIdAndCreatedAtAfter
CREATE INDEX IF NOT EXISTS idx_cost_ledger_workspace_status
    ON workspace_cost_ledger_entries (workspace_id, status, created_at);
