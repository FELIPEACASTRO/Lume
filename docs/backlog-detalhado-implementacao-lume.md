# Backlog Detalhado de Implementacao da Lume v2.0

Data base: 2026-03-07

## 1. Como ler este backlog

Este backlog traduz o roadmap v2.0 em trilhas executaveis. A ordem oficial continua:

1. rebase documental
2. shell e tenancy
3. projects/settings/scheduled/search/library
4. task execution
5. AI aggregation layer
6. skills/connectors/integrations/mail/browser
7. usage, billing e compliance

## 2. Tranche ja implementada neste pacote

### FE + BE + DATA

- `projects` reais no backend e no frontend
- `tasks` reais com task view e steps persistidos
- `usage` no topo da shell e pagina dedicada
- `inbox` real com notificacoes de workspace
- `settings` com shell lateral interna e overview real
- `search` ampliada para `projects`, `tasks`, `settings`, `usage` e `inbox`
- `workspace summary` ampliado com contadores de projetos, tarefas e notificacoes

## 3. Backlog por fase

## Fase 0. Rebase documental e benchmark

### P0

- reclassificar o benchmark Manus em `observado`, `inferido`, `externo`, `proposto`
- mapear cada modulo do Manus para `entra no MVP`, `entra apos o core` ou `nao entra agora`
- consolidar a taxonomia oficial v2.0

### P1

- documentar gaps formais entre `repo atual` e `produto-alvo`
- revisar backlog historico para remover escopo subdimensionado

## Fase 1. Shell equivalente e organizacao real

### P0

- fechar `projects`, `tasks`, `usage`, `inbox` e `settings` como superficies estaveis
- integrar topbar com `usage`, `notifications` e `settings`
- concluir search multi-origem sobre rotas e dados reais
- garantir acessibilidade de drawer, palette e task view

### P1

- refinar `Projects` com ownership detalhado e filtros
- refinar `Tasks` com filtros por tipo, projeto e status
- expor `shared links` reais para task view

## Fase 2. Identity, tenancy e settings de workspace

### P0

- auth B2B gerenciada
- `org/workspace/member/role/RBAC`
- workspace switcher
- member management

### P1

- `Conta`
- `Configuracoes`
- `Uso`
- `Tarefas agendadas`
- `Controles de dados`

### P2

- `Mail`
- `Navegador em nuvem`
- `Personalizacao`
- `Habilidades`
- `Conectores`
- `Integracoes`

## Fase 3. Biblioteca, artefatos e busca equivalente

### P0

- grid/list real para `Library`
- favoritos, tags, owners e source type
- busca sobre tarefas, projetos, biblioteca, settings e inbox

### P1

- compartilhamento de tarefas e artefatos
- data controls para itens compartilhados
- file assets e artifact links

## Fase 4. Task execution completa

### P0

- `POST /tasks` com tipos de task benchmark-driven
- mensagens da tarefa
- task runs
- step logs
- follow-up suggestions reais

### P1

- quick actions completas:
  - slides
  - sites
  - apps
  - design
  - research
  - spreadsheet
  - visualization
  - video
  - audio
  - chat
  - playbook

## Fase 5. AI Aggregation Layer

### P0

- provider registry
- model registry
- capability registry
- provider credentials
- unified inference
- routing/fallback/health

### P1

- BYOK
- request log
- budgets
- provider observability

## Fase 6. Skills, connectors e cloud operation

### P0

- connectors nativos
- integrations e webhooks
- skills oficiais e customizadas

### P1

- mail do workspace
- cloud browser

## Fase 7. Usage, billing e compliance

### P0

- usage metering real
- budgets
- entitlements
- plans/subscriptions
- invoices

### P1

- audit explorer
- retention
- export
- delete

## 4. Regras de implementacao

- nenhum item sai do backlog para o codigo sem `API + migration + teste`
- nenhum modulo preview perde o badge antes de ter backend real
- nenhum item do benchmark pode ser tratado como “ja coberto” so porque a shell parece parecida
- task execution e AI aggregation devem crescer em ordem, sem inverter IA profunda antes de `projects`, `tasks`, `search`, `settings` e `usage`
