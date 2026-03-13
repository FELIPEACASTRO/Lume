# Lume Workspace

Plataforma de produtividade com IA em arquitetura hibrida:

- `backend`: Spring Boot 3 + Java 21 + PostgreSQL
- `frontend`: Next.js 15 + TypeScript (App Router)

O frontend consome APIs reais do backend (`/api/v1/**`) e segue a base UX do roadmap: fluxo intuitivo, linguagem operacional e foco em produtividade.

## Estrutura

- `backend/`: dominio, servicos e APIs de workspace, providers, billing e FinOps
- `frontend/`: shell web, onboarding/login, home, chat, projetos, arquivos, prompts, assinatura, uso e admin
- `docs/`: documentacao funcional e tecnica
- `reports/insomnia/`: export Insomnia com cenarios de API

## Execucao local (Docker)

### Pre-requisitos

- Docker + Docker Compose
- Java 21
- Node 20+ (apenas para rodar frontend fora de Docker)

### Subida completa

```bash
docker compose up -d postgres backend frontend
```

### Endpoints

- Frontend: `http://localhost:3000`
- API base: `http://localhost:8080/api`
- Health: `http://localhost:8080/api/health`
- Swagger: `http://localhost:8080/api/swagger-ui.html`

## Fluxo inicial

1. Acesse `http://localhost:3000`.
2. Se o ambiente estiver vazio, conclua o setup inicial.
3. Faça login e navegue nas areas principais.

## Variaveis de ambiente

Use `.env.example` como referencia.

Minimo para desenvolvimento local:

- `DB_*`
- `SPRING_PROFILES_ACTIVE`
- `CORS_ALLOWED_ORIGINS` (ex.: `http://localhost:3000`)
- `NEXT_PUBLIC_API_BASE_URL` (ex.: `http://localhost:8080/api`)

Credenciais de providers continuam somente no backend (`.env` local, nunca no frontend).

## Desenvolvimento local sem Docker

Backend:

```bash
# PowerShell (split-origin local com frontend em localhost:3000)
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"

# Build + testes
mvn -q -f backend/pom.xml verify

# Subir API local
mvn -f backend/pom.xml spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Testes

Backend:

```bash
mvn -q -f backend/pom.xml test
mvn -q -f backend/pom.xml verify
```

Frontend:

```bash
cd frontend
npm run lint
npm run test
npm run build
```

E2E (com backend + frontend rodando localmente):

```bash
cd frontend
npm run test:e2e
```

## APIs utilizadas pelo frontend

O cliente web usa principalmente:

- `GET /api/v1/setup/status`
- `POST /api/v1/setup/bootstrap`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/session`
- `GET /api/v1/onboarding/current`
- `PATCH /api/v1/onboarding/current`
- `GET /api/v1/home/overview`
- `GET /api/v1/shell/navigation`
- `GET /api/v1/search/results`
- `GET /api/v1/providers/status`
- `GET /api/v1/models?provider=...`
- `GET /api/v1/agents/profiles`
- `GET /api/v1/agents/threads`
- `POST /api/v1/agents/threads`
- `POST /api/v1/agents/threads/{id}/messages`
- `GET /api/v1/projects`
- `POST /api/v1/projects`
- `PATCH /api/v1/projects/{id}`
- `GET /api/tasks`
- `POST /api/tasks`
- `GET /api/v1/knowledge-sources`
- `POST /api/v1/knowledge-sources`
- `PATCH /api/v1/knowledge-sources/{id}`
- `GET /api/v1/library/entries`
- `GET /api/v1/prompt-templates`
- `POST /api/v1/prompt-templates`
- `PATCH /api/v1/prompt-templates/{id}`
- `POST /api/v1/prompt-templates/{id}/touch`
- `GET /api/v1/billing/subscription`
- `POST /api/v1/billing/credit-packs/purchase`
- `POST /api/v1/billing/webhooks/mercado-pago`
- `GET /api/v1/billing/invoices`
- `GET /api/usage/summary`
- `GET /api/v1/finops/scorecard`
- `GET /api/v1/finops/anomalies`
- `GET/POST/PATCH /api/v1/support/tickets`
- `GET/POST/PATCH /api/v1/byok/connections`
- `POST /api/v1/byok/connections/{id}/validate`

## Insomnia

Colecao backend-only versionada:

- `reports/insomnia/lume-backend-only.insomnia.json`
