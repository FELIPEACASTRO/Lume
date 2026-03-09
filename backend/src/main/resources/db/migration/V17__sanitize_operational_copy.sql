UPDATE library_entries
SET status_label = 'Ativo'
WHERE status_label = 'API real';

UPDATE projects
SET status_label = 'Ativo'
WHERE status_label = 'API real';

UPDATE knowledge_sources
SET status_label = 'Ativo'
WHERE status_label = 'API real';

UPDATE agent_profiles
SET status_label = 'Configurado'
WHERE status_label = 'Agente versionado';

UPDATE agent_threads
SET status_label = 'Concluida'
WHERE status_label = 'Inferencia concluida';

UPDATE agent_threads
SET status_label = 'Com erro'
WHERE status_label = 'Falhou';
