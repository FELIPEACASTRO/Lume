ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS provider_code VARCHAR(80);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS model_code VARCHAR(160);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS version_label VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_tasks_workspace_provider
    ON tasks (workspace_id, provider_code);
