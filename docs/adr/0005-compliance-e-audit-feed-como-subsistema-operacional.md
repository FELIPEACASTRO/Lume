# ADR 0005 - Compliance e audit feed como subsistema operacional

## Status
Accepted

## Contexto
Settings de compliance sem trilha auditável e sem feed administrativo reduzem a confiança enterprise do produto.

## Decisão
Compliance e auditoria deixam de ser só resumo em settings e passam a expor um subsistema operacional mínimo:

- configuração persistida por workspace
- atualização auditada
- feed administrativo de auditoria
- boundary claro entre uso normal, mudanças administrativas e ações sensíveis

## Consequências
- superfícies live deixam de depender apenas de `note`
- admins conseguem investigar mudanças de policy, billing, support e BYOK
- readiness enterprise melhora sem replatform
