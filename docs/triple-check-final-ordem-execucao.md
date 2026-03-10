# Triple Check Final da Lume e Ordem Corrigida de Execucao

Data base: 2026-03-07

Este documento corrige a ordem de implementacao do programa da Lume e passa a ser a referencia executiva para sequenciamento, hygiene e gates tecnicos. Ele nao substitui o `roadmap-mestre-lume-ai-aggregation-platform.md`; ele o endurece.

## 1. Decisoes travadas

- `paralelo controlado` entre discovery e implementacao
- `managed B2B auth`
- `Stripe-first global`
- `LGPD/GDPR baseline` no MVP
- modalidades core obrigatorias: `texto + embeddings + image`
- `SSO/SCIM` apenas apos o core operacional
- Tier 1 oficial: `OpenAI + Anthropic + Google + xAI`
- bootstrap de busca: `Postgres full-text + search_documents + rebuild jobs`
- fundacao de jobs/eventos: `outbox + worker + retry + DLQ`

## 2. Gaps fechados pelo triple check

1. Higiene de repositorio antes de qualquer feature estrutural.
2. Drift entre docs, assumptions e estado real do repo.
3. Benchmark Manus ainda parcialmente acessivel.
4. Migracao do dominio atual de `users` para `identity-access` e `tenant-workspace`.
5. Entitlement tecnico separado de billing comercial.
6. Eventos e jobs com fundacao obrigatoria de entrega.
7. Search real com bootstrap definido.
8. Trilha local para BYOK, segredos, auth e billing.
9. Checklist formal de `preview -> live`.
10. Workstream permanente de `hygiene and debt control`.

## 3. Ordem cronologica obrigatoria

1. `Sprint 0`: hygiene, baseline e governanca de execucao
2. `F0A`: discovery critico e fechamento do benchmark acessivel
3. `F1A`: shell confiavel e sem mentira operacional
4. `F1B`: auth, tenancy e migracao do dominio atual
5. `F1C`: catalogo, segredos, observabilidade base e jobs
6. `F2A`: unified inference e connectors Tier 1
7. `F2B`: playground, prompts, metering, entitlements tecnicos e search real
8. `F3`: colaboracao, files, webhooks, benchmark e multimodalidade
9. `F4`: billing comercial e compliance
10. `F5`: auto-routing, failover e operacao global

## 4. Guardrails de implementacao

- Nenhuma implementacao continua enquanto houver drift entre docs, backlog, assumptions e estado real do repo.
- Nenhum modulo preview pode parecer live.
- Nenhuma API nova nasce sem versionamento, autorizacao, observabilidade e teste.
- Nenhum conector entra sem fonte oficial, contract test e rollback path.
- Nenhum modulo monetizavel sai de preview sem metering e entitlement.
- Nenhuma fase fecha com artefato gerado, import morto, fixture orfa ou documentacao defasada.

## 5. Tranche implementada neste repositorio

Este pacote iniciou a execucao do plano na fatia segura:

- `Sprint 0`: hygiene de `.gitignore` e remocao de artefato gerado do frontend
- `F1A`: shell mais honesta e coerente
  - topbar sem selo fixo de `Produto real`
  - drawer mobile removido da arvore de foco quando fechado
  - busca global sem atributo semantico incorreto nos resultados
  - exclusao de usuarios sem `window.confirm`
  - cards de metricas sem fingir zero durante loading ou erro
  - primitives novas: `ConfirmDialog`, `MetricCard`, `AsyncState`

## 6. Criterio de continuidade

A execucao so deve avancar para `F1B` depois de:

- repo sem sujeira gerada
- docs base alinhados a estas decisoes
- shell atual com smoke visual, mobile e keyboard aceitavel
- backlog e roadmap refletindo a ordem cronologica corrigida
