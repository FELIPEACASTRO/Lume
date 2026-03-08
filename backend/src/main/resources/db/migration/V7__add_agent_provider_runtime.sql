ALTER TABLE agent_profiles
    ADD COLUMN IF NOT EXISTS provider_code VARCHAR(80) NOT NULL DEFAULT 'openai';

ALTER TABLE agent_profiles
    ADD COLUMN IF NOT EXISTS model_code VARCHAR(120) NOT NULL DEFAULT 'openai:gpt-4.1-mini';

ALTER TABLE agent_profiles
    ADD COLUMN IF NOT EXISTS version_label VARCHAR(120) NOT NULL DEFAULT 'agent-v1-openai';

ALTER TABLE agent_profiles
    ADD COLUMN IF NOT EXISTS system_prompt TEXT NOT NULL DEFAULT 'Responda com foco operacional, clareza e proximos passos verificaveis.';

UPDATE agent_profiles
SET provider_code = 'openai',
    model_code = 'openai:gpt-4.1-mini',
    version_label = 'agent-v1-openai',
    system_prompt = 'Voce atua como um operador de processos. Estruture a resposta em diagnostico, riscos e proximo passo executavel.'
WHERE id = 'ops';

UPDATE agent_profiles
SET provider_code = 'anthropic',
    model_code = 'anthropic:claude-sonnet-4-5',
    version_label = 'agent-v1-claude',
    system_prompt = 'Voce atua como arquiteto de growth. Organize hipoteses, experimentos e mensagens com forte foco em clareza comercial.'
WHERE id = 'growth';

UPDATE agent_profiles
SET provider_code = 'google-gemini',
    model_code = 'google-gemini:gemini-2.5-pro',
    version_label = 'agent-v1-gemini-pro',
    system_prompt = 'Voce atua como analista de compliance. Priorize controle, risco, evidencias e linguagem conservadora.'
WHERE id = 'compliance';

UPDATE agent_profiles
SET provider_code = 'openai',
    model_code = 'openai:gpt-4.1-mini',
    version_label = 'agent-v1-openai'
WHERE id = 'strategy';
