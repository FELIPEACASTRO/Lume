CREATE TABLE IF NOT EXISTS auth_sessions (
    id              VARCHAR(96)  PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    user_agent      VARCHAR(255),
    ip_address      VARCHAR(64),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL,
    invalidated_at  TIMESTAMP,
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user ON auth_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_expires_at ON auth_sessions (expires_at);

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS runtime_state VARCHAR(32) NOT NULL DEFAULT 'queued';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE agent_threads ADD COLUMN IF NOT EXISTS runtime_state VARCHAR(32) NOT NULL DEFAULT 'idle';
ALTER TABLE agent_threads ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE users ALTER COLUMN organization_id DROP DEFAULT;
ALTER TABLE users ALTER COLUMN workspace_id DROP DEFAULT;

DELETE FROM task_steps WHERE task_id IN ('task-onboarding', 'task-site', 'task-research', 'task-strategy');
DELETE FROM tasks WHERE id IN ('task-onboarding', 'task-site', 'task-research', 'task-strategy');
DELETE FROM artifact_versions WHERE id IN (
    'ver-lib-onboarding-v1',
    'ver-lib-onboarding-v2',
    'ver-lib-governanca-v1',
    'ver-lib-memoria-v1',
    'ver-lib-strategy-brief-v1',
    'ver-lib-strategy-research-v1'
);
DELETE FROM prompt_templates WHERE id IN (
    'tpl-ops-onboarding',
    'tpl-growth-brief',
    'tpl-compliance-review',
    'tpl-strategy-thesis'
);
DELETE FROM knowledge_sources WHERE id IN (
    'knowledge-playbooks',
    'knowledge-agents',
    'knowledge-strategy'
);
DELETE FROM notifications WHERE id IN (
    'notif-task-onboarding',
    'notif-library-sync',
    'notif-agents',
    'notif-strategy'
);
DELETE FROM agent_messages WHERE thread_id IN (
    SELECT id
    FROM agent_threads
    WHERE workspace_id IN (
        SELECT id FROM workspaces WHERE slug IN ('workspace-principal', 'workspace-strategy')
    )
);
DELETE FROM agent_threads WHERE workspace_id IN (
    SELECT id FROM workspaces WHERE slug IN ('workspace-principal', 'workspace-strategy')
);
DELETE FROM agent_profiles WHERE id IN ('ops', 'growth', 'compliance', 'deepseek-research', 'grok-scout', 'sonar-briefing', 'strategy');
DELETE FROM library_entry_tags WHERE entry_id IN (
    'lib-onboarding',
    'lib-governanca',
    'lib-memoria',
    'lib-strategy-brief',
    'lib-strategy-research'
);
DELETE FROM library_entries WHERE id IN (
    'lib-onboarding',
    'lib-governanca',
    'lib-memoria',
    'lib-strategy-brief',
    'lib-strategy-research'
);
DELETE FROM projects WHERE id IN ('proj-ops', 'proj-growth', 'proj-strategy');

DELETE FROM memberships
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN ('operator@lume.local', 'ana.strategy@lume.local')
);

DELETE FROM user_preferences
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN ('operator@lume.local', 'ana.strategy@lume.local')
);

DELETE FROM auth_sessions
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN ('operator@lume.local', 'ana.strategy@lume.local')
);

DELETE FROM users
WHERE email IN ('operator@lume.local', 'ana.strategy@lume.local');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM workspaces WHERE slug = 'workspace-principal')
       AND NOT EXISTS (SELECT 1 FROM memberships WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM library_entries WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM projects WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM tasks WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM notifications WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal'))
       AND NOT EXISTS (SELECT 1 FROM knowledge_sources WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-principal')) THEN
        DELETE FROM workspaces WHERE slug = 'workspace-principal';
    END IF;

    IF EXISTS (SELECT 1 FROM workspaces WHERE slug = 'workspace-strategy')
       AND NOT EXISTS (SELECT 1 FROM memberships WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM library_entries WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM projects WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM tasks WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM notifications WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy'))
       AND NOT EXISTS (SELECT 1 FROM knowledge_sources WHERE workspace_id = (SELECT id FROM workspaces WHERE slug = 'workspace-strategy')) THEN
        DELETE FROM workspaces WHERE slug = 'workspace-strategy';
    END IF;

    IF EXISTS (SELECT 1 FROM organizations WHERE slug = 'lume')
       AND NOT EXISTS (SELECT 1 FROM workspaces WHERE organization_id = (SELECT id FROM organizations WHERE slug = 'lume')) THEN
        DELETE FROM organizations WHERE slug = 'lume';
    END IF;
END $$;
