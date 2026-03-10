# Architecture Overview

## Arquitetura real

O Lume continua um monolito Spring Boot backend-only.

- `users`: area mais proxima de Clean Architecture/CQRS/ACL
- `workspace/ai`: monolito modular em transicao, com Strategy/Registry/Adapter na camada de runtime de IA
- `workspace/*`: tenancy, settings, tasks, knowledge, library, agents, billing e FinOps

## Decisoes vigentes

- provider-by-provider continua capability-first
- nenhum provider usa segredo hardcoded
- metadata comercial, tier, limit e prioridade ficam fora do core
- threat-intel segue bloqueado por compliance e RBAC

## Boundary atual

- controllers versionados expoem shell, providers, settings, agents e capability API
- services de workspace concentram a fachada do produto
- `workspace/inference` concentra catalogo, adapters, resiliencia, metricas e secret resolution

## Target architecture

- curto prazo: aprofundar o monolito modular
- medio prazo: avaliar reactor multi-modulo somente se build/release pain, ownership e mudanca de cadencia justificarem
