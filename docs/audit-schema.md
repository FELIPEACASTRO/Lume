# Audit Schema

## ServiceAuditRecord

Contrato interno usado pela ferramenta em `tools/integration_audit/`.

| Campo | Tipo | Descricao |
|---|---|---|
| `service` | `string` | Nome canonico exibido no relatorio |
| `aliases` | `string[]` | Alias consolidados a partir dos catalogos |
| `category` | `string` | Categoria capability-first do Lume |
| `capabilities` | `string[]` | Capacidades inferidas para o servico |
| `implementationStatus` | `string` | Estado oficial reconciliado (`live`, `implemented_with_restrictions`, `catalog_only`, `blocked`, `out_of_scope`) |
| `integratedTestStatus` | `string` | Estado de readiness para teste integrado |
| `evidenceLevel` | `string` | Nivel de evidencia operacional |
| `exposure` | `string` | Onde o servico aparece (`backend`, `docs`, `config`, `tests`) |
| `coverageType` | `string` | Classificacao conservadora da cobertura (`NATIVE_IMPLEMENTED`, `INDIRECT_VIA_GATEWAY`, `PARTIAL`, `STUB_OR_MOCK`, `NOT_IMPLEMENTED`, `LEGACY_OR_DISABLED`) |
| `gatewayProvider` | `string?` | Evidencia textual de cobertura indireta via gateway, quando houver |
| `evidenceFiles` | `string[]` | Arquivos que sustentam a classificacao |
| `keySymbolsOrRoutes` | `string[]` | Simbolos, classes ou rotas capturadas no scanner |
| `primaryBlocker` | `string` | Bloqueador principal para readiness |
| `notes` | `string[]` | Observacoes adicionais, drift e notas do overlay |

## IntegratedTestStatus

- `READY_FOR_INTEGRATED_TEST`
- `READY_IF_SECRETS_PRESENT`
- `BLOCKED_BY_CONFIG`
- `BLOCKED_BY_BACKEND`
- `BLOCKED_BY_FEATURE_FLAG`
- `BLOCKED_BY_INFRA`
- `BLOCKED_BY_TEST_HARNESS`
- `UNKNOWN`

## CoverageType

- `NATIVE_IMPLEMENTED`
- `INDIRECT_VIA_GATEWAY`
- `PARTIAL`
- `STUB_OR_MOCK`
- `NOT_IMPLEMENTED`
- `LEGACY_OR_DISABLED`
