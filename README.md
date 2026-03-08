# Lume

Aplicacao full-stack com React 18, Spring Boot 3, Java 21 e PostgreSQL.

O repositorio hoje tem duas naturezas arquiteturais convivendo no mesmo monolito:

- `users` segue um desenho mais proximo de Clean Architecture com CQRS e ACL.
- `workspace/ai` e os modulos de shell/workspace seguem um monolito modularizado em transicao, com ports e adapters em partes do runtime de IA, mas ainda sem a mesma separacao do modulo legado de usuarios.

O objetivo desta documentacao e descrever o estado real da solucao, sem atribuir patterns ou garantias que o codigo ainda nao sustenta de ponta a ponta.

## Sumario

1. Arquitetura real
2. Patterns e principios realmente presentes
3. Estrutura do projeto
4. Execucao local
5. Variaveis de ambiente
6. Endpoints principais
7. Runtime multi-provider de IA
8. Testes e cobertura
9. Analise Big O honesta
10. Seguranca operacional
11. Fonte de verdade e governanca

## Arquitetura real

### Backend

O backend continua um monolito Spring Boot unico.

Partes principais:

- `domain`, `application`, `infrastructure` e `presentation`: base legada mais forte no modulo de usuarios.
- `workspace/*`: shell, settings, tenancy, members, providers, agents e capability API.
- `workspace/inference/*`: runtime multi-provider textual, catalogo, orquestracao, health, conectividade, seguranca de segredos e resiliência local.

Estado atual por area:

- `users`: mais aderente a Clean Architecture/CQRS/ACL.
- `workspace/ai`: monolito modularizado com Strategy/Registry/Adapter, ainda em transicao de acoplamento e separacao de responsabilidades.
- `research` e `threat-intel`: superficies separadas, com compliance explicito para threat-intel.

### Frontend

O frontend e uma SPA React com:

- componentes reutilizaveis
- paginas por rota
- services HTTP por dominio
- tipos TypeScript centralizados
- smoke e2e com Playwright
- unit tests com Vitest

O shell atual prioriza:

- tema `light/dark`
- shell estilo control-room
- command palette
- agents com runtime versionado
- settings com catalogo de providers e health/readiness

## Fonte de verdade e governanca

O Lume agora trata a governanca de providers e capabilities como parte do produto, nao como nota de rodape.

Ordem de precedencia quando houver conflito entre prompt, roadmap e runtime:

1. codigo executavel + testes verdes
2. `backend/src/main/resources/provider-governance-metadata.json`
3. `docs/ai-providers.md`, `docs/requirements-matrix.md`, `docs/provider-status-matrix.md` e `docs/provider-docs-matrix.md`
4. prompts e anexos de descoberta usados como insumo
5. documentacao oficial do fornecedor para fechar lacunas

Estados oficiais por provider:

- `live`
- `implemented_with_restrictions`
- `catalog_only`
- `blocked`
- `out_of_scope`

Niveis de evidencia:

- `offline_verified`
- `integration_verified`
- `online_verified`

## Patterns e principios realmente presentes

### Presentes e visiveis no codigo

- `Strategy`: `AiProviderAdapter`, `AuthStrategy`, `RequestShapeStrategy`, `ResponseExtractionStrategy`.
- `Registry`: `AiProviderRegistry`, `AiCircuitBreakerRegistry`, `AiBulkheadRegistry`, `AiRateLimiterRegistry`.
- `Adapter`: adapters por provider e adapters de persistencia/seguranca.
- `Factory/Builder`: factories e builders no modulo legado; construcao centralizada de catalogo/modelos no runtime.
- `Facade`: services de alto nivel como `InferenceGatewayService`, `ProviderConnectivityService`, `ProviderHealthService`.
- `ACL`: DTOs e mapeamentos no modulo legado e nos contratos versionados de workspace.

### Presentes apenas em parte do sistema

- `Clean Architecture`: forte no modulo legado de usuarios; parcial no runtime de workspace/ai.
- `CQRS`: real no modulo de usuarios; nao aplicado de forma global ao produto.
- `Ports and Adapters`: presente no runtime de IA e em partes do backend, mas nao uniforme em todo o repositorio.

### Nao devem ser assumidos como implementados globalmente

- microservices patterns como Saga
- event-driven architecture global
- CQRS global
- ACL global

## Estrutura do projeto

```text
Lume/
|-- backend/
|   |-- src/main/java/com/lume/
|   |   |-- domain/
|   |   |-- application/
|   |   |-- infrastructure/
|   |   |-- presentation/
|   |   `-- workspace/
|   |       |-- controller/
|   |       |-- dto/
|   |       |-- inference/
|   |       `-- service/
|   |-- src/main/resources/
|   |   |-- application.yml
|   |   `-- db/migration/
|   `-- pom.xml
|-- frontend/
|   |-- src/
|   |   |-- components/
|   |   |-- pages/
|   |   |-- routes/
|   |   |-- services/
|   |   |-- test/
|   |   `-- types/
|   |-- tests/
|   `-- package.json
|-- docs/
|   `-- ai-providers.md
`-- docker-compose.yml
```

## Execucao local

### Pre-requisitos

- Docker + Docker Compose
- Java 21
- Maven 3.9+
- Node.js 22+
- pnpm

### Subir o banco

```bash
docker compose up -d postgres
```

### Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend:

- API base: `http://localhost:8080/api`
- Health: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### Frontend

```bash
cd frontend
pnpm install
pnpm dev --host 127.0.0.1 --port 4173
```

Frontend:

- `http://127.0.0.1:4173`

## Variaveis de ambiente

Use `.env.example` como referencia. `.env` deve continuar fora do git.

### Infra

| Variavel | Descricao |
|---|---|
| `DB_HOST` | host do PostgreSQL |
| `DB_PORT` | porta do PostgreSQL |
| `DB_NAME` | nome do banco |
| `DB_USERNAME` | usuario do banco |
| `DB_PASSWORD` | senha do banco |
| `SPRING_PROFILES_ACTIVE` | perfil Spring |
| `VITE_API_URL` | base URL da API no frontend |

### IA textual e research

| Variavel | Descricao |
|---|---|
| `OPENAI_API_KEY` | OpenAI |
| `GEMINI_API_KEY` | Gemini |
| `DEEPSEEK_API_KEY` | DeepSeek |
| `ANTHROPIC_API_KEY` | Anthropic |
| `XAI_API_KEY` | xAI |
| `PERPLEXITY_API_KEY` | Perplexity |
| `GROQ_API_KEY` | Groq |
| `OPENROUTER_API_KEY` | OpenRouter |
| `TOGETHER_API_KEY` | Together |
| `FIREWORKS_API_KEY` | Fireworks |
| `DEEPINFRA_API_KEY` | DeepInfra |
| `MISTRAL_API_KEY` | Mistral |
| `COHERE_API_KEY` | Cohere |
| `EXA_API_KEY` | Exa |
| `NEWSCATCHER_API_KEY` | NewsCatcher |
| `VOYAGE_API_KEY` | Voyage AI |
| `TAVILY_API_KEY` | Tavily |
| `SERPAPI_API_KEY` | SerpApi |

Governanca por provider:

- `provider-governance-metadata.json` centraliza status de implementacao, nivel de evidencia, prioridade de negocio, sync mode, pricing summary, rate-limit summary e modos de roteamento
- `ProviderCatalogService` continua dono do contrato publico da API

### Threat-intel e compliance

| Variavel | Descricao |
|---|---|
| `SECURITY_COMPLIANCE_DARK_WEB_ENABLED` | habilita execucao threat-intel |
| `DARKOWL_PUBLIC_KEY` | credencial DarkOwl |
| `DARKOWL_PRIVATE_KEY` | credencial DarkOwl |
| `FULLHUNT_API_KEY` | FullHunt |
| `FLARE_API_KEY` | Flare |
| `FLARE_TENANT_ID` | tenant opcional Flare |

### Testes reais opcionais

| Variavel | Descricao |
|---|---|
| `RUN_REAL_AI_TESTS` | ativa smoke tests reais opt-in |
| `DEEPGRAM_TEST_AUDIO_URL` | audio remoto usado no smoke real de STT |
| `ASSEMBLYAI_TEST_AUDIO_URL` | audio remoto usado no smoke real de STT com AssemblyAI |
| `MISTRAL_OCR_DOCUMENT_URL` | documento remoto usado no smoke real de OCR |
| `IDEOGRAM_EDIT_IMAGE_URL` | imagem remota opcional para smoke real de edit |
| `IDEOGRAM_EDIT_MASK_URL` | mask remota opcional para smoke real de edit |
| `RUNWAY_TEST_IMAGE_URL` | frame remoto usado no smoke real de video |

## Endpoints principais

### Base da shell/workspace

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/health` |
| `GET` | `/api/workspace/summary` |
| `GET` | `/api/usage/summary` |
| `GET` | `/api/notifications` |
| `GET` | `/api/search` |

### Sessao, tenancy e members

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/v1/auth/session` |
| `GET` | `/api/v1/workspaces` |
| `POST` | `/api/v1/workspaces/{id}/activate` |
| `GET` | `/api/v1/members` |
| `POST` | `/api/v1/members` |
| `PATCH` | `/api/v1/members/{id}` |

### Settings e providers

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/settings/overview` |
| `GET` | `/api/v1/settings/preferences` |
| `PATCH` | `/api/v1/settings/preferences` |
| `GET` | `/api/v1/providers` |
| `GET` | `/api/v1/providers/{provider}` |
| `GET` | `/api/v1/models` |
| `GET` | `/api/v1/providers/{provider}/models` |
| `GET` | `/api/v1/provider-credentials` |
| `GET` | `/api/v1/providers/status` |
| `GET` | `/api/v1/providers/health` |
| `POST` | `/api/v1/providers/{code}/connectivity-test` |

### Inference e capability API

| Metodo | Endpoint |
|---|---|
| `POST` | `/api/v1/inference/execute` |
| `POST` | `/api/v1/inference/stream` |
| `POST` | `/api/v1/chat` |
| `POST` | `/api/v1/responses` |
| `POST` | `/api/v1/embeddings` |
| `POST` | `/api/v1/rerank` |
| `POST` | `/api/v1/images/generate` |
| `POST` | `/api/v1/images/edit` |
| `GET` | `/api/v1/images/jobs/{providerCode}/{jobId}` |
| `POST` | `/api/v1/videos/generate` |
| `GET` | `/api/v1/videos/jobs/{providerCode}/{jobId}` |
| `POST` | `/api/v1/audio/stt` |
| `POST` | `/api/v1/audio/tts` |
| `POST` | `/api/v1/ocr` |
| `POST` | `/api/v1/search` |
| `POST` | `/api/v1/web-grounded-chat` |
| `POST` | `/api/v1/threat-intel/search` |

Contratos capability-first nesta fase:

- requests de `chat`, `responses` e `web-grounded-chat` aceitam `provider`, `model`, `fallbackProviderCodes`, `requestId`, `routingMode`, `stream`, `tags` e `workspaceId`
- `web-grounded-chat` tambem aceita `researchProviderCode` e `searchLimit` para grounding explicito
- responses textuais agora retornam `requestedProviderCode`, `providerUsed`, `modelUsed`, `attemptChain` e `streamingMode`

### Agents

| Metodo | Endpoint |
|---|---|
| `GET` | `/api/agents/profiles` |
| `GET` | `/api/agents/threads` |
| `POST` | `/api/agents/threads` |
| `GET` | `/api/agents/threads/{id}/messages` |
| `POST` | `/api/agents/threads/{id}/messages` |
| `PATCH` | `/api/v1/agents/profiles/{id}/runtime` |

## Runtime multi-provider de IA

Providers com runtime textual realmente ligado nesta fase:

- `openai`
- `google-gemini`
- `deepseek`
- `anthropic`
- `xai`
- `perplexity`
- `groq`
- `openrouter`
- `cohere`
- `together`
- `fireworks`
- `deepinfra`
- `mistral`

Providers com capability runtime real adicional nesta fase:

- `voyage-ai` para `embeddings` e `rerank`
- `exa`, `newscatcher`, `tavily` e `serpapi` para `search`
- `deepgram` para `speech-to-text`
- `assemblyai` para `speech-to-text`
- `elevenlabs` para `text-to-speech`
- `mistral` para `ocr`
- `stability-ai` para `image generation`
- `replicate` para `image generation`, `image editing` e `video generation` assincronos com polling
- `ideogram` para `image generation` e `image editing`
- `bfl` para `image generation` e `image editing` assincronos com polling
- `runway` para `video generation` assincrona com polling interno

Providers em catalogo/manual/skeleton:

- `cloudflare-workers-ai`
- `azure-openai`
- `aws-bedrock`
- `hugging-face`
- `ai21`
- `cerebras`
- `nvidia-nim`
- `sambanova`
- `siliconflow`
- media/audio providers ainda fora desta rodada, exceto `deepgram`, `assemblyai`, `elevenlabs`, `mistral` OCR, `stability-ai`, `replicate`, `ideogram`, `bfl` e `runway`
- threat-intel providers que ainda nao tem adapter live

Overlay de governanca:

- `implementationStatus` diferencia `live` de `implemented_with_restrictions`, `catalog_only` e `blocked`
- `evidenceLevel` deixa explicito o nivel de prova disponivel
- `pricingSummary` e `rateLimitSummary` ficam em metadata externa, nao em constantes espalhadas no codigo

### Streaming

O endpoint SSE existe em `/api/v1/inference/stream`, mas a semantica agora e honesta:

- `native`: somente quando o adapter implementar streaming real
- `unsupported`: quando nao ha streaming real para o provider/modelo

Nao existe mais o comportamento de executar a resposta completa e fatiar localmente como se fosse stream real.

### Fallback

Fallback e sempre explicito por chamada.

Se `fallbackProviderCodes` estiver vazio:

- apenas o provider pedido e tentado

Se `fallbackProviderCodes` vier preenchido:

- a ordem enviada e respeitada
- cada tentativa gera metricas e logs separados

## Testes e cobertura

### Backend

```bash
cd backend
mvn test
mvn verify
```

Relatorio JaCoCo:

- `backend/target/site/jacoco/index.html`

Gate atualmente enforceado no build:

- linhas do bundle >= `70%`

Observacao:

- o repositorio ainda nao atingiu os thresholds arquiteturais mais altos discutidos no double-check.
- branch coverage do backend continua abaixo da meta aspiracional e precisa de mais testes de servico/orquestracao.

### Frontend

```bash
cd frontend
pnpm lint
pnpm test:unit
pnpm test:e2e
pnpm build
```

Estado atual:

- unit tests: componentes base, provider service, settings e agents
- e2e smoke: shell, search, workspace switch, tasks, library, users, settings, providers, agents e mobile sidebar

### Smoke tests reais opcionais

```bash
set RUN_REAL_AI_TESTS=true
mvn -Dtest=AiCapabilityRealSmokeIT test
```

Os testes reais:

- so rodam com `RUN_REAL_AI_TESTS=true`
- so exercitam providers com env var presente
- usam prompts minimos e baratos
- podem exercitar `cohere`, `voyage-ai`, `tavily`, `serpapi`, `deepgram`, `assemblyai`, `elevenlabs`, `mistral`, `stability-ai`, `ideogram`, `bfl` e `runway` quando as credenciais e URLs auxiliares estiverem presentes

## Analise Big O honesta

As operacoes do runtime de IA sao dominadas por I/O de rede. Ainda assim, a complexidade local relevante e:

| Operacao | Complexidade | Observacao |
|---|---|---|
| lookup de provider por codigo | `O(1)` | mapa indexado |
| lookup de modelo por codigo | `O(1)` | mapa indexado |
| listagem de modelos por provider | `O(k)` | `k` = modelos daquele provider, apos indexacao dedicada |
| cadeia de fallback | `O(F)` | `F` = numero de providers tentados |
| health agregado | `O(P)` | `P` = providers catalogados |
| connectivity snapshot lookup | `O(1)` | mapa em memoria |

Pontos importantes:

- o custo dominante de `sendPrompt`, `search` e `connectivity test` nao e CPU local; e latencia/upstream.
- snapshots de connectivity hoje sao efemeros e mantidos em memoria.

## Seguranca operacional

- segredos somente por env vars ou resolvedor de segredos
- `.env` fora do git
- nenhum provider deve hardcodar API key
- headers sensiveis nao devem ser logados
- prompts nao devem ser logados em claro em `INFO/WARN/ERROR`
- threat-intel continua `admin-only`, opt-in e dependente de compliance flag
- health agregado nao deve ser tratado como prova forte de disponibilidade externa; o snapshot atual e em memoria

## Documentacao adicional

- [AI Providers](docs/ai-providers.md)
- [Requirements Matrix](docs/requirements-matrix.md)
- [Provider Status Matrix](docs/provider-status-matrix.md)
- [Provider Docs Matrix](docs/provider-docs-matrix.md)
- [Official Sources](docs/official-sources.md)
- [Implementation Roadmap](docs/implementation-roadmap.md)
- [Architecture Overview](docs/architecture-overview.md)
- [Test Strategy](docs/test-strategy.md)
- [Test Evidence](docs/test-evidence.md)
- [Live Validation Matrix](docs/live-validation-matrix.md)
- [FinOps](docs/finops.md)
- [500 TPS Report](docs/performance/500tps-report.md)
- [Provider-by-provider](docs/provider-by-provider/README.md)
- [ADRs](docs/adr/README.md)

## Licenca

Repositorio privado.
