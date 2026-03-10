# Requirements Matrix

## Fonte de verdade e precedencia

Ao reconciliar conflito entre prompt, backlog, README e runtime, a precedencia no Lume e:

1. codigo executavel + testes verdes no repo
2. `backend/src/main/resources/provider-governance-metadata.json`
3. `docs/ai-providers.md` e `README.md`
4. artefatos externos de descoberta como insumo
5. documentacao oficial do fornecedor para fechar lacunas contratuais ou de autenticacao

## Taxonomia oficial de estado

| Campo | Valores | Uso |
|---|---|---|
| `implementationStatus` | `live`, `implemented_with_restrictions`, `catalog_only`, `blocked`, `out_of_scope` | estado de entrega do provider no produto |
| `runtimeMaturity` | `live`, `partial`, `catalog_only` | maturidade do runtime tecnico na API |
| `evidenceLevel` | `offline_verified`, `integration_verified`, `online_verified` | nivel de evidencia disponivel |
| `streamingMode` | `native`, `unsupported` | semantica real de streaming |

## Gates de release

| Gate | Obrigatorio |
|---|---|
| build backend | `mvn verify` |
| docs | matrizes atualizadas e README coerente com o estado real |
| provider catalog | nenhum provider marcado como `live` sem `implementationStatus` e `evidenceLevel` coerentes |
| threat-intel | bloqueado sem permissao, compliance flag e justificativa |
