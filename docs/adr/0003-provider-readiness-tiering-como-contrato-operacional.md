# ADR 0003 - Provider readiness e tiering como contrato operacional

## Status
Accepted

## Contexto
O Lume expõe um catálogo amplo de providers, mas a prontidão real varia por credencial, smoke live, restrição administrativa e maturidade de runtime. Quando docs, UI e API divergem, o produto parece menos confiável do que realmente é.

## Decisão
Provider readiness passa a ser tratado como contrato operacional explícito do produto.

- `providerTier` é a classificação canônica: `core_live`, `supported_restricted`, `catalog_only`, `blocked`
- `readinessStatus` continua descrevendo o estado técnico imediato
- `smokeStatus` descreve o estado de validação live
- `blockerCode` e `blockerMessage` explicam por que um provider ainda não está pronto para destaque operacional

## Consequências
- UI, docs e API precisam refletir o mesmo tier
- provider não pode parecer `live` quando falta credencial, smoke ou governança
- matrizes humanas deixam de ser fonte principal de verdade
