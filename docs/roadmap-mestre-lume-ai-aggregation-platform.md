# Roadmap Mestre da Lume v2.0 com Benchmark Manus

Data base: 2026-03-07

## 1. Norte do programa

- Benchmark funcional principal: `Manus`
- Fonte de verdade tecnica do ponto de partida: `repositorio atual do Lume`
- Posicionamento do produto: `AI orchestration + autonomous task execution + workspace operations`
- Modelo comercial: `B2B workspace-first`
- Identidade visual: `Lume premium propria`
- Stack preservada: `React + Vite`, `Spring Boot`, `PostgreSQL + Flyway`

## 2. Regras que nao mudam

- Nao copiar marca, assets, microcopy nem trade dress do Manus
- Separar sempre `observado diretamente`, `inferido com alta confianca`, `benchmark externo` e `decisao arquitetural proposta`
- Nenhum modulo sai de preview sem `API + persistencia + auditoria + observabilidade + testes`
- Nenhuma tela do frontend vira promessa silenciosa de backend inexistente
- Billing e creditos entram como camada de metering e governanca do workspace, nao como reposicionamento consumer-first

## 3. Nova taxonomia oficial da Lume

- `identity-access`
- `tenant-workspace`
- `workspace-shell`
- `projects-tasks`
- `task-execution`
- `artifacts-library`
- `search-discovery`
- `agents-messaging`
- `personalization-knowledge`
- `skills-extensions`
- `connectors-integrations`
- `provider-catalog`
- `inference-gateway`
- `usage-billing`
- `governance-compliance`

## 4. Ponto de partida real do repositorio

### O que ja existe

- shell React/Vite integrada com backend para `home`, `users`, `agents`, `library`
- contexto real de `auth/session`, `workspace summary`, `search`, `library`, `agents`
- organizacao e workspace iniciais em banco
- threads e mensagens de agents persistidas

### O que este rebase v2 acrescenta ja nesta tranche

- `projects` reais no backend e no frontend
- `tasks` reais com task view, steps e follow-up suggestions
- `usage` no topo da shell com API real
- `inbox` real com notificacoes do workspace
- `settings` com shell interna e overview conectado ao backend

## 5. Ordem cronologica oficial

### Fase 0. Rebase documental e matriz benchmark -> Lume

- reescrever roadmap e backlog usando o Manus como benchmark principal
- classificar cada modulo do Manus em:
  - `entra no MVP`
  - `entra apos o core`
  - `nao entra agora`
- consolidar taxonomia, gaps e ownership

### Fase 1. Fundacao do produto real e shell equivalente

- preservar `"/"`, `"/users"`, `"/agents"` e `"/library"`
- reorganizar a shell com:
  - `Projetos`
  - `Tarefas`
  - `Inbox`
  - `Usage`
  - `Settings`
- fechar command palette global, topbar com usage/notificacoes/avatar e shell honesta
- banco minimo:
  - `organizations`
  - `workspaces`
  - `users`
  - `memberships`
  - `roles`
  - `audit_logs`
  - `workspace_snapshots`
  - `recent_items`
  - `search_documents`
- resultado esperado:
  - a shell passa a refletir um workspace real, nao apenas uma vitrine coerente

### Fase 2. Identity, tenancy, settings e modulos de organizacao

- auth gerenciada B2B
- `org/workspace/member/role/RBAC`
- workspace switcher
- member management
- settings base equivalente ao benchmark, com escopo honesto
- modulos novos:
  - `Projects`
  - `Scheduled Tasks`
  - `Notifications/Inbox`
  - `Shared Links`
- banco:
  - `projects`
  - `project_members`
  - `tasks`
  - `task_runs`
  - `task_steps`
  - `task_schedules`
  - `notifications`
  - `shared_links`
  - `user_preferences`
  - `custom_instructions`
  - `knowledge_sources`

### Fase 3. Biblioteca, busca, artefatos e operacao documental

- expandir `Library` para grid/list, filtros, favoritos, owners e source type
- tornar a busca multi-origem equivalente ao benchmark
- adicionar data controls e compartilhamento de artefatos
- banco:
  - `library_entries`
  - `library_tags`
  - `library_entry_tags`
  - `file_assets`
  - `artifact_links`
  - `search_documents`
  - `data_control_items`

### Fase 4. Plataforma de execucao autonoma de tarefas

- composer principal benchmark-driven
- task threads
- plano de execucao por `steps`
- artefatos gerados por tarefa
- follow-ups
- quick actions como tipos de task:
  - `slides`
  - `sites`
  - `apps`
  - `design`
  - `research`
  - `spreadsheet`
  - `visualization`
  - `video`
  - `audio`
  - `chat`
  - `playbook`
- banco:
  - `task_messages`
  - `task_artifacts`
  - `task_plans`
  - `task_step_logs`
  - `task_followup_suggestions`

### Fase 5. AI Aggregation Layer e inferencia unificada

- `provider registry`
- `model registry`
- `capability registry`
- `provider credentials`
- `BYOK`
- `UnifiedRequest`
- `UnifiedResponse`
- `routing policies`
- `fallback policies`
- `provider health`
- Tier 1 oficial:
  - `OpenAI`
  - `Anthropic`
  - `Google`
  - `xAI`
- modalidades core:
  - `texto`
  - `embeddings`
  - `image`

### Fase 6. Skills, connectors, integrations e cloud operation

- `Skills` oficiais e customizadas
- `Connectors` nativos
- `Integrations`
- `Mail`
- `Cloud Browser`
- ordem de entrada:
  1. `connectors`
  2. `integrations`
  3. `skills`
  4. `mail`
  5. `cloud browser`

### Fase 7. Usage, billing, credits operacionais e compliance

- usage summary
- metering por request/task
- budgets
- entitlements
- plans/subscriptions
- invoices
- audit explorer
- retention/export/delete

## 6. Contratos publicos que o roadmap exige

### Frontend

- rotas base:
  - `"/"`
  - `"/users"`
  - `"/projects"`
  - `"/tasks"`
  - `"/tasks/:id"`
  - `"/agents"`
  - `"/library"`
  - `"/usage"`
  - `"/inbox"`
  - `"/settings"`

### Backend

- Fase 1:
  - `/auth/session`
  - `/workspace/summary`
  - `/search`
  - `/users`
  - `/projects`
  - `/tasks`
  - `/tasks/{id}`
  - `/library/entries`
  - `/agents/*`
  - `/usage/summary`
  - `/notifications`
  - `/settings/overview`
- Fase 5:
  - `/providers`
  - `/models`
  - `/credentials`
  - `/inference/execute`
  - `/budgets`
- Fase 6 e 7:
  - `/skills`
  - `/connectors`
  - `/webhooks`
  - `/mail`
  - `/billing`
  - `/compliance`

## 7. Gates de aceite

- nenhum modulo novo entra so com shell visual
- nenhuma rota nova entra sem contrato e migration
- nenhum item benchmark-driven sai do papel sem classificacao `live` ou `preview`
- task execution nao pode parecer autonoma enquanto a inferencia real nao existir
- `usage` e `credits` so entram como leitura operacional ou billing governado

## 8. Veredito executivo

O roadmap da Lume deixa de ser apenas o de um AI gateway com shell coerente e passa a ser o de uma plataforma completa de workspace operacional guiada por tarefas, artefatos e IA. O benchmark Manus redefine o produto-alvo; o repo atual continua sendo o baseline tecnico de onde a execucao parte.
