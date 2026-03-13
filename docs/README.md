# Docs Hub do Lume

Este diretorio concentra a documentacao vigente da plataforma Lume: frontend, backend, governanca de providers, arquitetura, testes, FinOps e matrizes de validacao.

## Ordem recomendada de leitura

1. `architecture-overview.md`
2. `requirements-matrix.md`
3. `ai-providers.md`
4. `provider-status-matrix.md`
5. `provider-docs-matrix.md`
6. `test-strategy.md`
7. `test-evidence.md`

## Artefatos principais

- `architecture-overview.md`
  - retrato honesto da arquitetura full-stack atual
- `requirements-matrix.md`
  - precedencia oficial, taxonomias e gates
- `ai-providers.md`
  - catalogo funcional de providers e capacidades
- `provider-status-matrix.md`
  - estado humano reconciliado por provider
- `provider-docs-matrix.md`
  - matriz documental por familia
- `official-sources.md`
  - links primarios para promocao de estado
- `test-strategy.md`
  - estrategia de testes da plataforma
- `test-evidence.md`
  - registro da evidencia executada
- `live-validation-matrix.md`
  - o que esta modelado, integrado ou validado online
- `finops.md`
  - principios de custo, quota e roteamento
- `provider-by-provider/`
  - perfis curtos dos providers prioritarios
- `adr/`
  - registros arquiteturais vigentes

## Fonte de verdade

1. codigo executavel + testes verdes
2. `backend/src/main/resources/provider-governance-metadata.json`
3. matrizes e docs deste diretorio
4. fontes oficiais dos fornecedores

## Regras de documentacao

- separar `observado diretamente`, `inferido`, `fonte externa` e `decisao arquitetural`
- nao declarar provider `live` sem evidencia no runtime e nos testes
- nao manter instrucoes ou claims de stack que nao existam mais no repositorio
