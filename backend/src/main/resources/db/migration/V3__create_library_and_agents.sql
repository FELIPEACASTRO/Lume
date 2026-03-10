CREATE TABLE IF NOT EXISTS library_entries (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    category      VARCHAR(120)  NOT NULL,
    status_label  VARCHAR(80)   NOT NULL,
    availability  VARCHAR(32)   NOT NULL,
    owner_name    VARCHAR(120)  NOT NULL,
    source_label  VARCHAR(120)  NOT NULL,
    summary       TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_library_entries_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE INDEX IF NOT EXISTS idx_library_entries_workspace_updated
    ON library_entries (workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS library_entry_tags (
    entry_id  VARCHAR(64)  NOT NULL,
    tag       VARCHAR(80)  NOT NULL,
    PRIMARY KEY (entry_id, tag),
    CONSTRAINT fk_library_entry_tags_entry
        FOREIGN KEY (entry_id) REFERENCES library_entries (id)
);

CREATE TABLE IF NOT EXISTS agent_profiles (
    id            VARCHAR(64)   PRIMARY KEY,
    workspace_id  BIGINT        NOT NULL,
    name          VARCHAR(140)  NOT NULL,
    specialty     VARCHAR(160)  NOT NULL,
    description   TEXT          NOT NULL,
    status_label  VARCHAR(80)   NOT NULL,
    availability  VARCHAR(32)   NOT NULL,
    note          TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_profiles_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
);

CREATE TABLE IF NOT EXISTS agent_threads (
    id                    VARCHAR(64)   PRIMARY KEY,
    workspace_id          BIGINT        NOT NULL,
    agent_profile_id      VARCHAR(64)   NOT NULL,
    title                 VARCHAR(200)  NOT NULL,
    status_label          VARCHAR(80)   NOT NULL,
    availability          VARCHAR(32)   NOT NULL,
    last_message_preview  TEXT,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_threads_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_agent_threads_profile
        FOREIGN KEY (agent_profile_id) REFERENCES agent_profiles (id)
);

CREATE INDEX IF NOT EXISTS idx_agent_threads_workspace_updated
    ON agent_threads (workspace_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS agent_messages (
    id          VARCHAR(64)   PRIMARY KEY,
    thread_id   VARCHAR(64)   NOT NULL,
    role        VARCHAR(20)   NOT NULL,
    body        TEXT          NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_messages_thread
        FOREIGN KEY (thread_id) REFERENCES agent_threads (id)
);

CREATE INDEX IF NOT EXISTS idx_agent_messages_thread_created
    ON agent_messages (thread_id, created_at ASC);

INSERT INTO library_entries (
    id,
    workspace_id,
    title,
    category,
    status_label,
    availability,
    owner_name,
    source_label,
    summary
)
SELECT
    'lib-onboarding',
    1,
    'Playbook de onboarding',
    'Playbook',
    'API real',
    'live',
    'Operacao',
    'Backend do workspace',
    'Fluxo mestre para criacao de usuarios, onboarding e comunicacao interna.'
WHERE NOT EXISTS (
    SELECT 1 FROM library_entries WHERE id = 'lib-onboarding'
);

INSERT INTO library_entries (
    id,
    workspace_id,
    title,
    category,
    status_label,
    availability,
    owner_name,
    source_label,
    summary
)
SELECT
    'lib-governanca',
    1,
    'Checklist de governanca',
    'Checklist',
    'API real',
    'live',
    'Financeiro',
    'Backend do workspace',
    'Lista de validacoes para acessos, politicas internas e evidencias.'
WHERE NOT EXISTS (
    SELECT 1 FROM library_entries WHERE id = 'lib-governanca'
);

INSERT INTO library_entries (
    id,
    workspace_id,
    title,
    category,
    status_label,
    availability,
    owner_name,
    source_label,
    summary
)
SELECT
    'lib-memoria',
    1,
    'Memoria de reunioes do time',
    'Memoria',
    'API real',
    'live',
    'Produto',
    'Backend do workspace',
    'Resumos acionaveis de reunioes com decisoes, riscos e owners registrados.'
WHERE NOT EXISTS (
    SELECT 1 FROM library_entries WHERE id = 'lib-memoria'
);

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-onboarding', 'usuarios'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-onboarding' AND tag = 'usuarios');

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-onboarding', 'compliance'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-onboarding' AND tag = 'compliance');

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-governanca', 'auditoria'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-governanca' AND tag = 'auditoria');

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-governanca', 'seguranca'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-governanca' AND tag = 'seguranca');

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-memoria', 'meeting'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-memoria' AND tag = 'meeting');

INSERT INTO library_entry_tags (entry_id, tag)
SELECT 'lib-memoria', 'roadmap'
WHERE NOT EXISTS (SELECT 1 FROM library_entry_tags WHERE entry_id = 'lib-memoria' AND tag = 'roadmap');

INSERT INTO agent_profiles (
    id,
    workspace_id,
    name,
    specialty,
    description,
    status_label,
    availability,
    note
)
SELECT
    'ops',
    1,
    'Ops Strategist',
    'Operacao e processos',
    'Traduz pedidos em fluxos executaveis com foco em custo, risco e velocidade.',
    'Preview assistido',
    'preview',
    'Threads e mensagens ja persistem no backend. A inferencia real entra na proxima fase.'
WHERE NOT EXISTS (
    SELECT 1 FROM agent_profiles WHERE id = 'ops'
);

INSERT INTO agent_profiles (
    id,
    workspace_id,
    name,
    specialty,
    description,
    status_label,
    availability,
    note
)
SELECT
    'growth',
    1,
    'Growth Architect',
    'Expansao e ativacao',
    'Estrutura campanhas, argumentos e alavancas de conversao com contexto do workspace.',
    'Preview assistido',
    'preview',
    'Threads e mensagens ja persistem no backend. A inferencia real entra na proxima fase.'
WHERE NOT EXISTS (
    SELECT 1 FROM agent_profiles WHERE id = 'growth'
);

INSERT INTO agent_profiles (
    id,
    workspace_id,
    name,
    specialty,
    description,
    status_label,
    availability,
    note
)
SELECT
    'compliance',
    1,
    'Compliance Analyst',
    'Controles e validacoes',
    'Valida regras, checkpoints e politicas antes de publicar qualquer fluxo.',
    'Preview assistido',
    'preview',
    'Threads e mensagens ja persistem no backend. A inferencia real entra na proxima fase.'
WHERE NOT EXISTS (
    SELECT 1 FROM agent_profiles WHERE id = 'compliance'
);
