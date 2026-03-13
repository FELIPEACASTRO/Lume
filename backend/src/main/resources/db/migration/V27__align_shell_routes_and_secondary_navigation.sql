UPDATE shell_navigation_items
SET path = '/projetos', updated_at = CURRENT_TIMESTAMP
WHERE id = 'projects';

UPDATE shell_navigation_items
SET path = '/arquivos', updated_at = CURRENT_TIMESTAMP
WHERE id = 'library';

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'chat', 'Chat', '/chat', 'Conversas e threads operacionais do workspace.', 'chat', 'live', 'secondary', 70, TRUE, 'chat,threads,conversas'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'chat');

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'prompts', 'Prompts', '/prompts', 'Templates, variaveis e ativos reutilizaveis.', 'prompts', 'live', 'secondary', 80, TRUE, 'prompts,templates,reuso'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'prompts');

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'billing', 'Assinatura', '/assinatura', 'Plano, invoices e packs do workspace.', 'billing', 'live', 'secondary', 90, TRUE, 'assinatura,billing,invoices'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'billing');

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'usage', 'Uso', '/uso', 'Consumo, limites e custos operacionais.', 'usage', 'live', 'secondary', 100, TRUE, 'uso,custos,consumo'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'usage');

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'admin', 'Admin / FinOps', '/admin', 'Governanca, providers, BYOK e sinais financeiros.', 'admin', 'restricted', 'secondary', 110, TRUE, 'admin,finops,providers,byok'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'admin');

INSERT INTO shell_navigation_items (id, label, path, description, icon, availability, nav_group, sort_order, enabled, keywords_raw)
SELECT 'help', 'Ajuda', '/ajuda', 'Tickets, suporte e historico de atendimento.', 'help', 'live', 'secondary', 120, TRUE, 'ajuda,suporte,tickets'
WHERE NOT EXISTS (SELECT 1 FROM shell_navigation_items WHERE id = 'help');
