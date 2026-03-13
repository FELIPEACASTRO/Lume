# ADR 0004 - Billing e ledgers separados como boundary de produto

## Status
Accepted

## Contexto
O Lume precisa operar billing e FinOps de forma auditável. Misturar estados financeiros, crédito do cliente e custo interno torna reconciliação, suporte e governança mais frágeis.

## Decisão
O produto mantém quatro trilhas explícitas:

- `usage ledger`
- `cost ledger`
- `credit ledger`
- entidades financeiras (`invoice`, `payment_event`, `reconciliation_run`)

## Consequências
- crédito não pode ser aplicado sem evento financeiro válido
- scorecard, suporte e reconciliação operam sobre dados separados
- status financeiro e status operacional não se misturam nos contratos
