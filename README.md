# Lume Backend

API Spring Boot 3 com Java 21 e PostgreSQL para operacao de workspace, providers de IA, billing e FinOps.

O repositorio esta em modo `backend-only`. O antigo cliente web legado foi removido e nao faz mais parte do fluxo operacional, da documentacao corrente nem dos comandos de build.

## Arquitetura

- monolito Spring Boot unico
- `users`: modulo legado mais proximo de Clean Architecture/CQRS
- `workspace/*`: tenancy, settings, members, tasks, knowledge, library, agents e capability APIs
- `workspace/inference/*`: catalogo, adapters por provider, health, connectivity, roteamento e resolucao de segredos

## Execucao local

### Pre-requisitos

- Docker + Docker Compose
- Java 21
- Maven 3.9+

### Fluxo recomendado

```bash
docker compose up -d postgres
mvn -q -f backend/pom.xml verify
docker compose build --no-cache backend
docker compose up -d postgres backend
```

### Endpoints de bootstrap e saude

- API base: `http://localhost:8080/api`
- Health: `http://localhost:8080/api/health`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### Primeira inicializacao

1. `GET /api/v1/setup/status`
2. Se `setupRequired=true`, faca `POST /api/v1/setup/bootstrap`
3. Depois autentique com `POST /api/v1/auth/login`
4. Confirme a sessao com `GET /api/v1/auth/session`

## Variaveis de ambiente

Use `.env.example` como referencia. O `.env` local nao deve ser versionado.

### Infra

| Variavel | Descricao |
|---|---|
| `DB_HOST` | host do PostgreSQL |
| `DB_PORT` | porta do PostgreSQL |
| `DB_NAME` | nome do banco |
| `DB_USERNAME` | usuario do banco |
| `DB_PASSWORD` | senha do banco |
| `SPRING_PROFILES_ACTIVE` | perfil Spring |
| `BILLING_WEBHOOK_SECRET` | segredo HMAC para validar webhooks de billing |
| `CORS_ALLOWED_ORIGINS` | lista opcional de origens separadas por virgula para acesso cross-origin |

### Providers e capabilities

O arquivo `.env.example` mantem a lista completa de credenciais suportadas por familia:

- texto/chat
- embeddings/rerank
- search/research
- audio/OCR/midia
- threat-intel
- smoke tests reais opt-in

## Endpoints principais

### Sistema e autenticacao

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/health` |
| `GET` | `/api/v1/setup/status` |
| `POST` | `/api/v1/setup/bootstrap` |
| `POST` | `/api/v1/auth/login` |
| `GET` | `/api/v1/auth/session` |
| `POST` | `/api/v1/auth/logout` |

### Workspace e acesso

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/v1/workspaces` |
| `POST` | `/api/v1/workspaces/{id}/activate` |
| `GET` | `/api/v1/members` |
| `POST` | `/api/v1/members` |
| `PATCH` | `/api/v1/members/{id}` |

### Providers e modelos

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/v1/providers` |
| `GET` | `/api/v1/providers/{provider}` |
| `GET` | `/api/v1/models` |
| `GET` | `/api/v1/providers/{provider}/models` |
| `GET` | `/api/v1/provider-credentials` |
| `GET` | `/api/v1/providers/status` |
| `GET` | `/api/v1/providers/health` |
| `POST` | `/api/v1/providers/{code}/connectivity-test` |

### Capability APIs

| Metodo | Endpoint |
|---|---|
| `POST` | `/api/v1/inference/execute` |
| `POST` | `/api/v1/inference/stream` |
| `POST` | `/api/v1/chat` |
| `POST` | `/api/v1/responses` |
| `POST` | `/api/v1/embeddings` |
| `POST` | `/api/v1/rerank` |
| `POST` | `/api/v1/search` |
| `POST` | `/api/v1/web-grounded-chat` |
| `POST` | `/api/v1/translation/text` |
| `POST` | `/api/v1/nlp/analyze` |
| `POST` | `/api/v1/audio/stt` |
| `POST` | `/api/v1/audio/tts` |
| `POST` | `/api/v1/ocr` |
| `POST` | `/api/v1/images/generate` |
| `POST` | `/api/v1/images/edit` |
| `GET` | `/api/v1/images/jobs/{providerCode}/{jobId}` |
| `POST` | `/api/v1/videos/generate` |
| `GET` | `/api/v1/videos/jobs/{providerCode}/{jobId}` |
| `POST` | `/api/v1/threat-intel/search` |

### Produto, billing e FinOps

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/v1/home/overview` |
| `GET` | `/api/v1/shell/navigation` |
| `GET` | `/api/settings/overview` |
| `GET` | `/api/v1/settings/preferences` |
| `PATCH` | `/api/v1/settings/preferences` |
| `GET` | `/api/v1/budgets/current` |
| `PATCH` | `/api/v1/budgets/current` |
| `GET` | `/api/v1/billing/subscription` |
| `PATCH` | `/api/v1/billing/subscription` |
| `POST` | `/api/v1/billing/credit-packs/purchase` |
| `POST` | `/api/v1/billing/webhooks/provider-event` |
| `GET` | `/api/v1/billing/invoices` |
| `GET` | `/api/v1/billing/payment-events` |
| `GET` | `/api/v1/finops/scorecard` |
| `GET` | `/api/v1/finops/ledgers/credits` |
| `GET` | `/api/v1/finops/ledgers/costs` |
| `GET` | `/api/v1/finops/events/usage` |
| `GET` | `/api/v1/finops/reconciliation` |
| `POST` | `/api/v1/finops/reconciliation/run` |
| `GET` | `/api/v1/finops/reconciliation/history` |
| `GET` | `/api/v1/onboarding/current` |
| `PATCH` | `/api/v1/onboarding/current` |

## Testes

### Backend

```bash
mvn -q -f backend/pom.xml test
mvn -q -f backend/pom.xml verify
```

### Docker

```bash
docker compose config --no-interpolate
docker compose up -d postgres backend
```

## Documentacao

- [architecture-overview.md](docs/architecture-overview.md)
- [requirements-matrix.md](docs/requirements-matrix.md)
- [test-strategy.md](docs/test-strategy.md)
- [test-evidence.md](docs/test-evidence.md)
- [ai-providers.md](docs/ai-providers.md)

## Insomnia

O export versionado da API fica em:

- `reports/insomnia/lume-backend-only.insomnia.json`

Ele cobre:

- setup e autenticacao reais
- providers/modelos/health/connectivity
- capability APIs
- produto, billing e FinOps
- pasta separada para endpoints publicos fora de `/api/v1`
