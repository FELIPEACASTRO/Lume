ALTER TABLE knowledge_sources
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

ALTER TABLE knowledge_sources
    ADD COLUMN IF NOT EXISTS source_uri VARCHAR(512);

ALTER TABLE knowledge_sources
    ADD COLUMN IF NOT EXISTS document_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE knowledge_sources
    ADD COLUMN IF NOT EXISTS enabled_for_agents BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE knowledge_sources
    ADD COLUMN IF NOT EXISTS last_indexed_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_knowledge_sources_project'
          AND table_name = 'knowledge_sources'
    ) THEN
        ALTER TABLE knowledge_sources
            ADD CONSTRAINT fk_knowledge_sources_project
                FOREIGN KEY (project_id) REFERENCES projects (id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_knowledge_sources_workspace_project_updated
    ON knowledge_sources (workspace_id, project_id, updated_at DESC);
