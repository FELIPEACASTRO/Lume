UPDATE agent_profiles
SET status_label = 'Configurado',
    availability = 'attention',
    note = 'O agente esta configurado e depende das credenciais do provedor selecionado.'
WHERE id IN ('ops', 'growth', 'compliance')
  AND status_label = 'Preview assistido';

UPDATE knowledge_sources
SET status_label = 'Contexto ativo',
    availability = 'live',
    note = 'Threads e mensagens do workspace disponiveis para consulta operacional.'
WHERE id = 'knowledge-agents'
  AND status_label = 'Preview assistido';

UPDATE tasks
SET summary = 'Tarefa registrada para planejamento e execucao do site de campanha.'
WHERE id = 'task-site'
  AND summary = 'Tarefa registrada como execucao assistida antes da inferencia real.';

UPDATE task_steps
SET title = 'Preparar execucao',
    detail = 'A tarefa esta registrada e pronta para o proximo passo operacional.'
WHERE task_id = 'task-onboarding'
  AND step_order = 3
  AND title = 'Preparar execucao assistida';
