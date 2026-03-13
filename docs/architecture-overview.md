# Architecture Overview

## Arquitetura real

O Lume opera como plataforma full-stack com backend Spring Boot e frontend Next.js.

- `users`: area mais proxima de Clean Architecture/CQRS/ACL
- `workspace/ai`: monolito modular em transicao, com Strategy/Registry/Adapter na camada de runtime de IA
- `workspace/*`: tenancy, settings, tasks, knowledge, library, agents, billing e FinOps
- `frontend/*`: App Router, shell workspace-first, design system e integracao typed com `/api/v1`

## Decisoes vigentes

- provider-by-provider continua capability-first
- nenhum provider usa segredo hardcoded
- metadata comercial, tier, limit e prioridade ficam fora do core
- threat-intel segue bloqueado por compliance e RBAC

## Boundary atual

- controllers versionados expoem shell, providers, settings, agents e capability API
- services de workspace concentram a fachada do produto
- `workspace/inference` concentra catalogo, adapters, resiliencia, metricas e secret resolution
- frontend consome sessao por cookie e nunca manipula segredos de providers

## Target architecture

- curto prazo: aprofundar o monolito modular
- medio prazo: avaliar reactor multi-modulo somente se build/release pain, ownership e mudanca de cadencia justificarem
