ALTER TABLE user_preferences
    ADD COLUMN IF NOT EXISTS active_workspace_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_user_preferences_active_workspace
    ON user_preferences (active_workspace_id);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'workspace.read'
FROM roles
WHERE roles.code = 'workspace_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'workspace.read'
);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'workspace.switch'
FROM roles
WHERE roles.code = 'workspace_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'workspace.switch'
);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'members.read'
FROM roles
WHERE roles.code = 'workspace_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'members.read'
);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'members.manage'
FROM roles
WHERE roles.code = 'workspace_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'members.manage'
);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'workspace.read'
FROM roles
WHERE roles.code = 'workspace_member'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'workspace.read'
);

INSERT INTO role_permissions (role_id, permission_key)
SELECT roles.id, 'workspace.switch'
FROM roles
WHERE roles.code = 'workspace_member'
  AND NOT EXISTS (
    SELECT 1
    FROM role_permissions
    WHERE role_permissions.role_id = roles.id
      AND role_permissions.permission_key = 'workspace.switch'
);
