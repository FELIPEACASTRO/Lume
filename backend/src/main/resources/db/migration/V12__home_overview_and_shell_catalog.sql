CREATE TABLE IF NOT EXISTS shell_navigation_items (
    id            VARCHAR(64) PRIMARY KEY,
    label         VARCHAR(120) NOT NULL,
    path          VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    icon          VARCHAR(64) NOT NULL,
    availability  VARCHAR(32) NOT NULL,
    nav_group     VARCHAR(32) NOT NULL,
    sort_order    INTEGER     NOT NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    keywords_raw  TEXT        NOT NULL DEFAULT '',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shell_task_types (
    task_type     VARCHAR(64) PRIMARY KEY,
    label         VARCHAR(120) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    sort_order    INTEGER     NOT NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELETE FROM shell_navigation_items;
INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
VALUES
    ('home', 'Início', '/', 'Resumo do trabalho e atalhos do workspace.', 'home', 'live', 'primary', 10, TRUE, 'inicio,resumo,atividade,atalhos'),
    ('tasks', 'Tarefas', '/tasks', 'Acompanhe o que está em andamento.', 'tasks', 'live', 'primary', 20, TRUE, 'tarefas,execucao,erros,board'),
    ('projects', 'Projetos', '/projects', 'Organize entregas, responsáveis e contexto.', 'projects', 'live', 'primary', 30, TRUE, 'projetos,contexto,ownership,backlog'),
    ('library', 'Biblioteca', '/library', 'Arquivos, entregas e histórico do trabalho.', 'library', 'live', 'primary', 40, TRUE, 'biblioteca,arquivos,entregas,versoes'),
    ('users', 'Equipe', '/users', 'Gerencie pessoas, papéis e acesso.', 'users', 'live', 'primary', 50, TRUE, 'equipe,membros,acesso,rbac'),
    ('settings', 'Configurações', '/settings', 'Ajustes, uso, providers e conhecimento.', 'settings', 'live', 'primary', 60, TRUE, 'configuracoes,uso,providers,knowledge');

DELETE FROM shell_task_types;
INSERT INTO shell_task_types (task_type, label, description, sort_order, enabled)
VALUES
    ('research', 'Pesquisar', 'Levantar contexto, fontes e decisões.', 10, TRUE),
    ('playbook', 'Criar playbook', 'Definir um processo reutilizável.', 20, TRUE),
    ('sites', 'Criar página', 'Organizar uma página, site ou landing.', 30, TRUE),
    ('apps', 'Planejar app', 'Estruturar fluxo, backlog e módulos.', 40, TRUE),
    ('design', 'Organizar design', 'Reunir referências e entregáveis visuais.', 50, TRUE),
    ('slides', 'Montar apresentação', 'Criar narrativa e estrutura de slides.', 60, TRUE);
