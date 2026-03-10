CREATE TABLE IF NOT EXISTS home_overview_settings (
    id              VARCHAR(32) PRIMARY KEY,
    headline        VARCHAR(180) NOT NULL,
    supporting_text VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS home_overview_blocks (
    id            VARCHAR(64) PRIMARY KEY,
    block_type    VARCHAR(64) NOT NULL UNIQUE,
    title         VARCHAR(120) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    sort_order    INTEGER     NOT NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELETE FROM home_overview_settings;
INSERT INTO home_overview_settings (id, headline, supporting_text)
VALUES ('default', 'O que voce quer fazer?', 'Busque informacoes do workspace, abra uma nova tarefa e acompanhe o que pede atencao.');

DELETE FROM home_overview_blocks;
INSERT INTO home_overview_blocks (id, block_type, title, description, sort_order, enabled)
VALUES
    ('in-progress', 'in_progress', 'Em andamento', 'O que precisa de voce agora.', 10, TRUE),
    ('alerts', 'alerts', 'Alertas', 'O que mudou e precisa de atencao.', 20, TRUE),
    ('team-context', 'team_context', 'Equipe e contexto', 'Quem cuida do trabalho e onde encontrar contexto.', 30, TRUE),
    ('recent', 'recent', 'Recentes', 'Volte para onde parou.', 40, TRUE),
    ('quick-links', 'quick_links', 'Acesso rapido', 'Abra as areas principais do workspace.', 50, TRUE);
