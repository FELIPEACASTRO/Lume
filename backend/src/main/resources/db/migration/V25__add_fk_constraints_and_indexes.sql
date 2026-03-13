-- Foreign key constraints for referential integrity on new tables

ALTER TABLE support_tickets
    ADD CONSTRAINT fk_support_tickets_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_byok_connections
    ADD CONSTRAINT fk_byok_connections_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_compliance_settings
    ADD CONSTRAINT fk_compliance_settings_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

-- Index for SLA breach queries (tickets by workspace + user for dashboards)
CREATE INDEX IF NOT EXISTS idx_support_tickets_workspace_user
    ON support_tickets (workspace_id, created_by_user_id);
