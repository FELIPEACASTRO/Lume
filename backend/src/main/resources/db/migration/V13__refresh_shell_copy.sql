UPDATE shell_navigation_items
SET
    label = CASE id
        WHEN 'home' THEN 'Inicio'
        WHEN 'tasks' THEN 'Tarefas'
        WHEN 'projects' THEN 'Projetos'
        WHEN 'library' THEN 'Biblioteca'
        WHEN 'users' THEN 'Equipe'
        WHEN 'settings' THEN 'Configuracoes'
        ELSE label
    END,
    description = CASE id
        WHEN 'home' THEN 'Resumo do trabalho e atalhos do workspace.'
        WHEN 'tasks' THEN 'Acompanhe o que esta em andamento.'
        WHEN 'projects' THEN 'Organize entregas, responsaveis e contexto.'
        WHEN 'library' THEN 'Arquivos, entregas e historico do trabalho.'
        WHEN 'users' THEN 'Gerencie pessoas, funcoes e acesso.'
        WHEN 'settings' THEN 'Ajustes, uso, conhecimento e provedores.'
        ELSE description
    END,
    keywords_raw = CASE id
        WHEN 'home' THEN 'inicio,resumo,atividade,atalhos'
        WHEN 'tasks' THEN 'tarefas,andamento,erros,lista'
        WHEN 'projects' THEN 'projetos,contexto,responsaveis,backlog'
        WHEN 'library' THEN 'biblioteca,arquivos,entregas,versoes'
        WHEN 'users' THEN 'equipe,membros,acesso,rbac'
        WHEN 'settings' THEN 'configuracoes,uso,provedores,knowledge'
        ELSE keywords_raw
    END,
    updated_at = CURRENT_TIMESTAMP;

UPDATE shell_task_types
SET
    label = CASE task_type
        WHEN 'research' THEN 'Pesquisar'
        WHEN 'playbook' THEN 'Criar playbook'
        WHEN 'sites' THEN 'Criar pagina'
        WHEN 'apps' THEN 'Planejar app'
        WHEN 'design' THEN 'Organizar design'
        WHEN 'slides' THEN 'Montar apresentacao'
        ELSE label
    END,
    description = CASE task_type
        WHEN 'research' THEN 'Levantar contexto, fontes e decisoes.'
        WHEN 'playbook' THEN 'Definir um processo reutilizavel.'
        WHEN 'sites' THEN 'Organizar uma pagina, site ou landing.'
        WHEN 'apps' THEN 'Estruturar fluxo, backlog e modulos.'
        WHEN 'design' THEN 'Reunir referencias e entregas visuais.'
        WHEN 'slides' THEN 'Criar narrativa e estrutura de slides.'
        ELSE description
    END,
    updated_at = CURRENT_TIMESTAMP;
