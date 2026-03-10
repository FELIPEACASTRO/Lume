# Docs Hub do Lume

Este diretorio concentra os artefatos de diagnostico, estrategia e execucao do Lume.

## Ordem recomendada de leitura

1. `roadmap-mestre-lume-ai-aggregation-platform.md`
2. `backlog-detalhado-implementacao-lume.md`
3. `triple-check-final-ordem-execucao.md`
4. `double-check-lume-100-especialistas.md`
5. `painel-100-especialistas-lume.md`
6. `fase-0-pacote-mestre-lume.md`
7. `register-perguntas-lacunas-lume.md`

## Artefatos atuais

### Governanca operacional e evidencia

- `requirements-matrix.md`
  - taxonomias oficiais de estado, evidencia e gates
  - matriz por capability e precedencia de fontes

- `provider-status-matrix.md`
  - estado humano do catalogo por provider
  - usado para reconciliar runtime, roadmap e onboarding

- `provider-docs-matrix.md`
  - proveniencia documental por familia
  - risco de onboarding e tipo de fonte

- `official-sources.md`
  - links primarios por provider/familia
  - referencia obrigatoria para promocao de estado

- `implementation-roadmap.md`
  - roadmap incremental capability-first
  - fases e gates atuais do runtime

- `architecture-overview.md`
  - retrato honesto da arquitetura atual
  - target architecture e limites da fase

- `test-strategy.md`
  - estrategia de teste por nivel de evidencia
  - suites, gates e responsabilidades

- `test-evidence.md`
  - como registrar prova de teste e promocao de provider

- `live-validation-matrix.md`
  - o que esta apenas modelado, integrado ou validado online

- `finops.md`
  - principios de custo, quota e roteamento

- `performance/500tps-report.md`
  - status da meta de throughput e plano de benchmark

- `provider-by-provider/`
  - perfis curtos dos providers prioritarios

- `adr/`
  - registros arquiteturais vigentes

### Diagnostico e critica especializada

- `double-check-lume-100-especialistas.md`
  - diagnostico executivo do frontend atual
  - matriz de consenso e dissenso
  - backlog de correcao do shell

- `triple-check-final-ordem-execucao.md`
  - correcoes do triple check
  - ordem cronologica obrigatoria
  - guardrails de hygiene, docs e gates tecnicos

- `painel-100-especialistas-lume.md`
  - painel com 100 micro-pareceres
  - lentes de UX, IA, acessibilidade, navegacao, design system e produto

### Estrategia e execucao

- `roadmap-mestre-lume-ai-aggregation-platform.md`
  - roadmap oficial da Lume v2.0 com benchmark Manus
  - fases, modulos, dominios, gates e ordem cronologica

- `backlog-detalhado-implementacao-lume.md`
  - backlog executavel alinhado ao benchmark Manus
  - usado para quebrar implementacao em tranches reais de frontend, backend e dados

- `fase-0-pacote-mestre-lume.md`
  - detalhamento operacional da Fase 0
  - entregaveis, evidencias, criterios de aceite e trilha documental

- `register-perguntas-lacunas-lume.md`
  - registro formal de duvidas, lacunas e premissas
  - uso obrigatorio durante descoberta, benchmark e arquitetura

## Fonte de verdade

- `Repositorio atual`
  - ponto de partida tecnico real
- `Benchmark Manus`
  - referencia funcional principal
- `Pesquisa em fontes oficiais`
  - obrigatoria para provedores, modelos, billing, seguranca, limites e integracoes

## Regras de documentacao

- Separar sempre `observado diretamente`, `inferido com alta confianca`, `benchmark externo` e `decisao arquitetural proposta`.
- Nenhuma funcionalidade de frontend entra no escopo final sem backend, persistencia, observabilidade e teste definidos.
- Nenhuma integracao de IA entra em roadmap executivo sem fonte oficial e data de verificacao registradas.
