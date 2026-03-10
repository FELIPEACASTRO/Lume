# FinOps

## Principios

- custo, quota, tier e rate limit ficam em metadata externa
- routing pode privilegiar `cost-first`, `latency-first` ou `quality-first`
- custo deve ser agregado por workspace, provider e capability
- jobs caros de media e threat-intel exigem feature flag e limite operacional

## Leituras obrigatorias no produto

- custo estimado por chamada quando disponivel
- uso por workspace/provider/capability
- retries, timeouts e fallback como sinal de custo indireto

## Regras

- nao hardcodar preco no core
- nao usar README para declarar economia sem medicao
- providers enterprise e paid so sobem com owner tecnico e owner financeiro claros

## Endpoints operacionais

- `GET /api/v1/billing/subscription`
- `PATCH /api/v1/billing/subscription`
- `POST /api/v1/billing/credit-packs/purchase`
- `POST /api/v1/billing/webhooks/provider-event` (header `X-Lume-Billing-Signature`)
- `GET /api/v1/billing/invoices`
- `GET /api/v1/billing/payment-events`
- `GET /api/v1/finops/scorecard`
- `GET /api/v1/finops/ledgers/credits?limit=20`
- `GET /api/v1/finops/ledgers/costs?limit=20`
- `GET /api/v1/finops/events/usage?limit=20`
- `GET /api/v1/finops/reconciliation`
- `POST /api/v1/finops/reconciliation/run`
- `GET /api/v1/finops/reconciliation/history?limit=20`

## Modelo de ledger

- `credit_ledger`: saldo do cliente por workspace (`grant`, `adjustment`, `pack_purchase`, `overage`).
- `cost_ledger`: custo operacional estimado por chamada (`provider`, `model`, `capability`, `status`, `latency`).
- `usage_events`: trilha de uso funcional para auditoria operacional.

## Reconciliacao financeira

- `GET /api/v1/finops/reconciliation` retorna o diagnostico atual sem alterar saldo.
- `POST /api/v1/finops/reconciliation/run` executa reconciliacao operacional.
- `POST /api/v1/finops/reconciliation/run` com body `{"applyCreditFix": true}` permite ajuste automatico do drift entre assinatura e `credit_ledger`.
- `GET /api/v1/finops/reconciliation/history` retorna historico persistido de execucoes (`manual` e `system`).
- reconciliacao agendada mensal opcional:
  - `lume.finops.reconciliation.enabled`
  - `lume.finops.reconciliation.cron`
  - `lume.finops.reconciliation.auto-fix-credit-drift`
  - `lume.finops.reconciliation.max-workspaces-per-run`

## Seguranca de webhook

- segredo central: `BILLING_WEBHOOK_SECRET`
- validacao HMAC SHA-256 em payload canonico
- idempotencia por `workspace_id + gateway_event_id`
