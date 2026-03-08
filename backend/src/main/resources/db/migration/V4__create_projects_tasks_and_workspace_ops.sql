CREATE TABLE IF NOT EXISTS projects (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    name          VARCHAR(160)  NOT NULL,
    status_label  VARCHAR(80)   NOT NULL,
    availability  VARCHAR(32)   NOT NULL,
    owner_name    VARCHAR(120)  NOT NULL,
    summary       TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_projects_workspace_updated
    ON projects (workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS tasks (
    id             VARCHAR(64)   PRIMARY KEY,
    workspace_id   BIGINT        NOT NULL,
    project_id     VARCHAR(64),
    task_type      VARCHAR(64)   NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    prompt         TEXT          NOT NULL,
    summary        TEXT          NOT NULL,
    status_label   VARCHAR(80)   NOT NULL,
    availability   VARCHAR(32)   NOT NULL,
    owner_name     VARCHAR(120)  NOT NULL,
    scheduled_for  TIMESTAMP,
    share_slug     VARCHAR(120),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id)
);

CREATE INDEX IF NOT EXISTS idx_tasks_workspace_updated
    ON tasks (workspace_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_tasks_workspace_project
    ON tasks (workspace_id, project_id);

CREATE TABLE IF NOT EXISTS task_steps (
    id            VARCHAR(64)   PRIMARY KEY,
    task_id        VARCHAR(64)   NOT NULL,
    step_order     INTEGER       NOT NULL,
    step_type      VARCHAR(64)   NOT NULL,
    title          VARCHAR(180)  NOT NULL,
    detail         TEXT          NOT NULL,
    status_label   VARCHAR(32)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_steps_task
        FOREIGN KEY (task_id) REFERENCES tasks (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_task_steps_task_order
    ON task_steps (task_id, step_order);

CREATE TABLE IF NOT EXISTS notifications (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    kind          VARCHAR(80)   NOT NULL,
    title         VARCHAR(180)  NOT NULL,
    body          TEXT          NOT NULL,
    path          VARCHAR(255)  NOT NULL,
    is_read       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_workspace_created
    ON notifications (workspace_id, created_at DESC);

CREATE TABLE IF NOT EXISTS shared_links (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    entity_type   VARCHAR(64)   NOT NULL,
    entity_id     VARCHAR(64)   NOT NULL,
    slug          VARCHAR(120)  NOT NULL UNIQUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shared_links_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id              BIGSERIAL     PRIMARY KEY,
    user_id         BIGINT        NOT NULL UNIQUE,
    appearance      VARCHAR(32)   NOT NULL DEFAULT 'system',
    language_code   VARCHAR(20)   NOT NULL DEFAULT 'pt-BR',
    email_updates   BOOLEAN       NOT NULL DEFAULT TRUE,
    product_updates BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS custom_instructions (
    id            BIGSERIAL     PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL UNIQUE,
    profile_bio   TEXT          NOT NULL DEFAULT '',
    occupation    VARCHAR(160)  NOT NULL DEFAULT '',
    instructions  TEXT          NOT NULL DEFAULT '',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_custom_instructions_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE TABLE IF NOT EXISTS knowledge_sources (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    title         VARCHAR(180)  NOT NULL,
    source_type   VARCHAR(80)   NOT NULL,
    status_label  VARCHAR(80)   NOT NULL,
    availability  VARCHAR(32)   NOT NULL,
    note          TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_sources_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_sources_workspace_updated
    ON knowledge_sources (workspace_id, updated_at DESC);
