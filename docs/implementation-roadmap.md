# Implementation Roadmap

## Estado atual

- monolito modular com runtime textual, vetorial e search parcialmente live
- governanca e evidencia por provider ja expostas na API
- gateways enterprise e threat-intel avancado ainda dependem de tranches dedicadas; STT/TTS/OCR e image/video basicos ja sairam do estado `unsupported`
- budgets operacionais por workspace, cost center e modo `showback|chargeback` agora entram no shell de `usage` e `settings`

## Recorte de valor

### core_now

- workspace-first B2B com tenancy, RBAC, projects, tasks, library, inbox, settings e usage estaveis
- FinOps operacional com budgets, cost center e guardrails por workspace
- runtime capability-first para `chat`, `responses`, `embeddings`, `rerank`, `search`, grounded chat e media basica

### next_after_core

- knowledge plane permissionado
- prompt/template library e artifact versions
- BYOK, virtual API keys e workflow runtime com approvals

### parked

- marketplace publico, revenue share, white-label amplo
- browser/computer use, telefonia e canais avancados
- claims de `500 TPS` sem benchmark real

## Fases

### Fase 0. Verdade operacional e recorte de valor

- reconciliar prompts, anexos e docs oficiais
- manter `provider-governance-metadata.json` como overlay oficial
- atualizar matrizes e README a cada mudanca de estado
- usar a auditoria read-only em `tools/integration_audit/` como mecanismo de evidencia e drift detection; relatorios derivados nao sao fonte primaria e nao devem ser versionados
- manter `core_now`, `next_after_core` e `parked` como backlog curado do produto

### Fase 1. Fundacao B2B e FinOps operacional

- consolidar workspace, roles, budgets, cost centers e guardrails por workspace
- manter budgets como camada operacional, sem fingir faturamento comercial final
- expor `budgets.read` e `budgets.manage` com leitura no shell e gestao em settings

### Fase 2. Runtime capability-first

- consolidar `chat`, `responses`, `embeddings`, `rerank`, `search` e `web-grounded-chat`
- expor `routingMode`, `attemptChain`, `requestedProviderCode` e metadados de evidencia no contrato
- remover qualquer semantica enganosa de streaming

### Fase 3. Media e audio

- expandir STT, TTS, OCR, image generation, image editing e video generation para mais providers
- introduzir lifecycle padrao de jobs assincronos
- persistir e renderizar artifacts de forma consistente

### Fase 4. Gateways enterprise

- Bedrock, Azure OpenAI, Cloudflare Workers AI, GitHub Models, Hugging Face e AI21
- readiness honesto antes de declarar qualquer provider enterprise como live

### Fase 5. Knowledge plane, artifacts e workflows

- retrieval hibrido, citations permissionadas e memoria por projeto/workspace/agente
- prompt/template library, artifact versions e forks
- workflow runtime com replay/retry/approval e runtime de artifacts

### Fase 6. Threat-intel avancado

- FullHunt e Flare primeiro
- DarkOwl, Twingly e correlatos depois de compliance e onboarding

### Fase 7. Performance, billing e decisao arquitetural

- medir throughput real progressivo
- fechar custo por workspace/provider/capability
- decidir com evidencia se reactor multi-modulo passa a valer o custo
