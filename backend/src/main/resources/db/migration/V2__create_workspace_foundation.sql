CREATE TABLE IF NOT EXISTS organizations (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(160)  NOT NULL,
    slug        VARCHAR(160)  NOT NULL UNIQUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspaces (
    id              BIGSERIAL     PRIMARY KEY,
    organization_id BIGINT        NOT NULL,
    name            VARCHAR(160)  NOT NULL,
    slug            VARCHAR(160)  NOT NULL UNIQUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspaces_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

INSERT INTO organizations (name, slug)
SELECT 'Lume', 'lume'
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE slug = 'lume'
);

INSERT INTO workspaces (organization_id, name, slug)
SELECT 1, 'Workspace Principal', 'workspace-principal'
WHERE NOT EXISTS (
    SELECT 1 FROM workspaces WHERE slug = 'workspace-principal'
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS workspace_id BIGINT;

UPDATE users
SET organization_id = 1
WHERE organization_id IS NULL;

UPDATE users
SET workspace_id = 1
WHERE workspace_id IS NULL;

ALTER TABLE users ALTER COLUMN organization_id SET DEFAULT 1;
ALTER TABLE users ALTER COLUMN workspace_id SET DEFAULT 1;

ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN workspace_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id);

CREATE INDEX IF NOT EXISTS idx_users_workspace ON users (workspace_id);
CREATE INDEX IF NOT EXISTS idx_users_org_workspace ON users (organization_id, workspace_id);

CREATE TABLE IF NOT EXISTS roles (
    id           BIGSERIAL     PRIMARY KEY,
    code         VARCHAR(80)   NOT NULL UNIQUE,
    name         VARCHAR(120)  NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id         BIGINT       NOT NULL,
    permission_key  VARCHAR(120) NOT NULL,
    PRIMARY KEY (role_id, permission_key),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

INSERT INTO roles (code, name, description)
SELECT 'workspace_admin', 'Workspace Admin', 'Acesso administrativo ao workspace'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'workspace_admin'
);

INSERT INTO roles (code, name, description)
SELECT 'workspace_member', 'Workspace Member', 'Acesso operacional ao workspace'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'workspace_member'
);

CREATE TABLE IF NOT EXISTS memberships (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    organization_id BIGINT      NOT NULL,
    workspace_id    BIGINT      NOT NULL,
    role_id         BIGINT      NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_memberships_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_memberships_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_memberships_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_memberships_unique_active
    ON memberships (user_id, workspace_id);

INSERT INTO memberships (user_id, organization_id, workspace_id, role_id)
SELECT users.id, users.organization_id, users.workspace_id, 1
FROM users
WHERE NOT EXISTS (
    SELECT 1
    FROM memberships
    WHERE memberships.user_id = users.id
      AND memberships.workspace_id = users.workspace_id
);

CREATE TABLE IF NOT EXISTS feature_flags (
    id              BIGSERIAL     PRIMARY KEY,
    organization_id BIGINT,
    workspace_id    BIGINT,
    flag_key        VARCHAR(120)  NOT NULL,
    enabled         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feature_flags_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_feature_flags_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_feature_flags_scope
    ON feature_flags (organization_id, workspace_id, flag_key);

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL     PRIMARY KEY,
    organization_id BIGINT        NOT NULL,
    workspace_id    BIGINT        NOT NULL,
    actor_user_id   BIGINT,
    entity_type     VARCHAR(80)   NOT NULL,
    entity_id       VARCHAR(80)   NOT NULL,
    action          VARCHAR(80)   NOT NULL,
    payload         TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_audit_logs_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_created_at
    ON audit_logs (workspace_id, created_at DESC);
