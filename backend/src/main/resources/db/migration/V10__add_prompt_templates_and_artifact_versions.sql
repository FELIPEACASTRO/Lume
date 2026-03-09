ALTER TABLE library_entries
    ADD COLUMN IF NOT EXISTS entry_type VARCHAR(64) NOT NULL DEFAULT 'artifact';

ALTER TABLE library_entries
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

ALTER TABLE library_entries
    ADD COLUMN IF NOT EXISTS favorited BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE library_entries
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_library_entries_project'
          AND table_name = 'library_entries'
    ) THEN
        ALTER TABLE library_entries
            ADD CONSTRAINT fk_library_entries_project
                FOREIGN KEY (project_id) REFERENCES projects (id);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS artifact_versions (
    id               VARCHAR(64)  PRIMARY KEY,
    entry_id         VARCHAR(64)  NOT NULL,
    workspace_id     BIGINT       NOT NULL,
    version_label    VARCHAR(80)  NOT NULL,
    change_summary   VARCHAR(240) NOT NULL,
    content_preview  TEXT         NOT NULL,
    created_by_name  VARCHAR(120) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_artifact_versions_entry
        FOREIGN KEY (entry_id) REFERENCES library_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_artifact_versions_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_artifact_versions_entry_created
    ON artifact_versions (entry_id, created_at DESC);

CREATE TABLE IF NOT EXISTS prompt_templates (
    id                VARCHAR(64)  PRIMARY KEY,
    workspace_id      BIGINT       NOT NULL,
    project_id        VARCHAR(64),
    agent_profile_id  VARCHAR(64),
    title             VARCHAR(200) NOT NULL,
    summary           TEXT         NOT NULL,
    prompt_body       TEXT         NOT NULL,
    template_scope    VARCHAR(64)  NOT NULL DEFAULT 'workspace',
    status_label      VARCHAR(80)  NOT NULL,
    availability      VARCHAR(32)  NOT NULL,
    owner_name        VARCHAR(120) NOT NULL,
    variables_raw     TEXT         NOT NULL DEFAULT '',
    favorited         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_used_at      TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_templates_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_prompt_templates_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_prompt_templates_agent
        FOREIGN KEY (agent_profile_id) REFERENCES agent_profiles (id)
);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_workspace_updated
    ON prompt_templates (workspace_id, updated_at DESC);
