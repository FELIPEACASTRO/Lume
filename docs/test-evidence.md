# Test Evidence

## Evidencia mantida no repo

- relatórios de cobertura backend em `backend/target/site/jacoco/`
- relatórios de cobertura frontend via Vitest
- smoke e2e em `frontend/tests/`

## Evidencia da rodada atual

Comandos executados nesta rodada:

- `backend`: `mvn -q -Dtest=AudioDocumentCapabilityServiceTest,AiPlatformControllerIT,ProviderCatalogServiceTest,ProviderCatalogIT,AiCapabilityServiceTest test`
- `backend`: `mvn -q "-Dtest=MediaCapabilityServiceTest,AiPlatformControllerIT,ProviderCatalogServiceTest,ProviderCatalogIT,AiCapabilityServiceTest" test`
- `backend`: `mvn -q "-Dtest=MediaCapabilityServiceTest,AiPlatformControllerIT,ProviderCatalogServiceTest,ProviderCatalogIT,AiCapabilityRealSmokeIT" test`
- `backend`: `mvn -q test`
- `backend`: `mvn -q verify`
- `frontend`: `pnpm lint`
- `frontend`: `pnpm test`
- `frontend`: `pnpm build`
- `frontend`: `pnpm test:unit:coverage`

Resultado:

- backend: verde em `test` e `verify`
- frontend: verde em `lint`, `unit`, `e2e`, `build` e `coverage`
- backend coverage atual: `80%` linhas, `47%` branches
- frontend coverage atual: `21.01%` linhas, `50.56%` branches

Capacidades validadas nesta rodada:

- `speech-to-text` com Deepgram
- `speech-to-text` com AssemblyAI
- `text-to-speech` com ElevenLabs
- `ocr` com Mistral OCR
- `image-generation` com Ideogram
- `image-generation` assincrona com Replicate
- `image-generation` assincrona com BFL
- `image-editing` assincrona com Replicate
- `image-editing` com Ideogram
- `image-editing` assincrona com BFL
- `video-generation` assincrona com Replicate
- `video-generation` assincrona com Runway
- `image-generation` sincrona com Stability AI

Observacao:

- os testes passaram, mas os thresholds aspiracionais do roadmap ainda nao foram atingidos, principalmente no frontend

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
| threat-intel | compliance gate + integration + evidência de redaction |
