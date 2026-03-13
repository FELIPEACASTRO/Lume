# Test Evidence

## Evidencia mantida no repo

- relatorios de cobertura backend em `backend/target/site/jacoco/`
- relatórios derivados gerados em `reports/`

## Evidencia da rodada atual

Comandos executados nesta rodada:

- `backend`: `mvn -q -f backend/pom.xml verify`
- `backend`: IT dedicada para `POST /api/v1/billing/webhooks/mercado-pago` (assinatura, idempotencia, payload/status invalido)
- `backend`: IT de operacao cobrindo scorecard, configuracoes, suporte e shell
- `frontend`: `npm --prefix frontend run lint`
- `frontend`: `npm --prefix frontend run test`
- `frontend`: `npm --prefix frontend run build`
- `frontend`: `npm --prefix frontend run test:e2e` (jornadas criticas setup/login, first-value e admin)
- `docker compose config --no-interpolate`

Resultado:

- backend: verde em `test` e `verify`
- frontend: lint + unit tests + build de producao verdes
- frontend: E2E verde em ambiente local com proxy same-origin para `/backend-api`
- compose: valido para `postgres + backend + frontend`
- validacao local split-origin concluida com `http://localhost:3000` consumindo `http://localhost:8080/api`
- ajuste de CORS aplicado no backend para liberar `localhost:3000` e `127.0.0.1:3000` por default em ambiente local
- endurecimento do cliente HTTP aplicado:
  - timeout padrao de API
  - `Content-Type` apenas quando ha body
  - mensagens de erro de startup mais honestas na tela inicial
- E2E critico revalidado localmente em `2026-03-11` com 3/3 cenarios verdes:
  - `setup/login/session`
  - `onboarding first-value`
  - `admin flow (finops, byok, support)`

## Evidencia da rodada de 2026-03-12

Comandos executados nesta rodada:

- `backend`: `mvn -q -f backend/pom.xml clean verify`
- `backend`: rerun direcionado de ITs criticas
  - `AuthenticationFilterIT`
  - `IdentityTenancyIT`
  - `MercadoPagoWebhookIT`
- `frontend`: `npm --prefix frontend run lint`
- `frontend`: `npm --prefix frontend run test`
- `frontend`: `npm --prefix frontend run build`
- `frontend`: `npm --prefix frontend run test:e2e`

Correcoes validadas:

- `LoginRateLimiterFilter` passou a resolver o path real da request, inclusive quando `servletPath` vem vazio no MockMvc.
- `AuthenticationFilterIT` passou a rodar com `lume.auth.allow-test-auto-login=false`, alinhando o contrato de 401 nos cenarios sem sessao.
- Webhooks de billing em IT agora usam `occurredAt` fresco, compatível com a janela anti-replay de 300s.
- O H2 de teste passou a usar nome unico por contexto (`application-test.yml`), eliminando contaminacao entre contextos Spring com `create-drop`.
- As ITs versionadas usam `"/v1/..."` em MockMvc, evitando roteamento acidental para `ResourceHttpRequestHandler`.

Resultado:

- backend: `clean verify` verde
- frontend: lint + unit tests + build verdes
- frontend: E2E verde com `3/3`
- pacote de release local voltou a ficar coerente entre backend, frontend e testes de integracao

## Evidencia da rodada de 2026-03-12 (zero-mock frontend)

Comandos executados nesta rodada:

- `backend`: `mvn -q -f backend/pom.xml verify`
- `frontend`: `npm run lint` (em `frontend/`)
- `frontend`: `npm run build` (em `frontend/`)
- `frontend`: `npm run test` (Playwright integraçao real sem `vi.mock`)
- `frontend`: `npm run test:e2e`
- varredura de mocks: `rg -n "vi\.mock|jest\.mock|mockResolvedValue|__mocks__" frontend`

Correcoes validadas:

- novo contrato backend para catálogos dinâmicos: `GET /api/v1/settings/ui-options`
- frontend passou a consumir opções dinâmicas do backend em:
  - `home` (onboarding)
  - `users` (roles)
  - `ajuda` (categoria/severidade/status)
  - `arquivos` (tipo de fonte)
  - `assinatura` (packs)
  - `admin` (providers/scope de BYOK)
  - `settings` (status de compliance)
- removido fallback de navegação estática em `WorkspaceNav`; quando backend não entrega itens, a UI mostra estado de indisponibilidade.
- removido fallback de sessão com `"Operador"`/`"Workspace"` no `AppShell`.
- suíte de testes mockada (Vitest + RTL) removida; `npm run test` agora executa integração real via Playwright.
- ajustes E2E para refletir estado real da UI backend-driven (sem assert legado de `"Workspace ativo"`).

Resultado:

- backend: `verify` verde
- frontend: lint sem warnings/erros
- frontend: build verde
- frontend: `test` verde (3/3)
- frontend: `test:e2e` verde (3/3)
- busca por mocks em `frontend/`: sem ocorrências nos arquivos ativos

## Regras

- teste verde sem documento de estado nao autoriza promover provider sozinho
- provider promovido precisa ter sua linha atualizada nas matrizes
- smoke live so atualiza `online_verified` se rodar com credenciais reais e output registrado

## Evidencia minima por mudanca

| Mudanca | Evidencia minima |
|---|---|
| novo provider textual | unit + integration + connectivity test |
| novo provider vetorial | unit + integration |
| grounded chat | unit + integration com citations/grounding |
| provider enterprise | docs oficiais + readiness honesto + integration |
| threat-intel | compliance gate + integration + evidencia de redaction |
