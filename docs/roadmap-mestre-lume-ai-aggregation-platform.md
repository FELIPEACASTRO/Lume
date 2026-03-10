# Roadmap Mestre da Lume v2.0

Data base: 2026-03-07

## Norte do programa

- baseline tecnico: `repositorio atual do Lume`
- posicionamento do produto: `AI orchestration + autonomous task execution + workspace operations`
- modelo comercial: `B2B workspace-first`
- stack preservada: `Spring Boot`, `PostgreSQL + Flyway`

## O que ja existe

- contexto real de `auth/session`, `workspace summary`, `search`, `library` e `agents`
- organizacao e workspace iniciais em banco
- threads e mensagens de agents persistidas
- surfaces capability-first para texto, busca, vetorial, audio, OCR e media

## O que esta na trilha principal

- identity, tenancy e configuracao de workspace
- projects, tasks, library, knowledge e prompt templates
- provider registry, capability APIs, routing e observabilidade
- billing, budgets, ledgers e FinOps

## Gates

- nenhum modulo novo entra sem `API + persistencia + auditoria + observabilidade + testes`
- nenhum provider entra como `live` sem evidencia no runtime e nas matrizes
- billing e creditos so entram como governanca operacional do workspace
