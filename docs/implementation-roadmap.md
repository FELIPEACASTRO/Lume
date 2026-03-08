# Implementation Roadmap

## Estado atual

- monolito modular com runtime textual, vetorial e search parcialmente live
- governanca e evidencia por provider ja expostas na API
- gateways enterprise e threat-intel avancado ainda dependem de tranches dedicadas; STT/TTS/OCR e image/video basicos ja sairam do estado `unsupported`

## Fases

### Fase 0. Verdade operacional

- reconciliar prompts, anexos e docs oficiais
- manter `provider-governance-metadata.json` como overlay oficial
- atualizar matrizes e README a cada mudanca de estado

### Fase 1. Runtime capability-first

- consolidar `chat`, `responses`, `embeddings`, `rerank`, `search` e `web-grounded-chat`
- expor `routingMode`, `attemptChain`, `requestedProviderCode` e metadados de evidência no contrato
- remover qualquer semantica enganosa de streaming

### Fase 2. Media e audio

- expandir STT, TTS, OCR, image generation, image editing e video generation para mais providers
- introduzir lifecycle padrao de jobs assincronos
- persistir e renderizar artifacts de forma consistente

### Fase 3. Gateways enterprise

- Bedrock, Azure OpenAI, Cloudflare Workers AI, GitHub Models, Hugging Face e AI21
- readiness honesto antes de declarar qualquer provider enterprise como live

### Fase 4. Threat-intel avancado

- FullHunt e Flare primeiro
- DarkOwl, Twingly e correlatos depois de compliance e onboarding

### Fase 5. Performance e FinOps

- medir throughput real progressivo
- fechar custo por workspace/provider/capability
- decidir com evidencia se reactor multi-modulo passa a valer o custo
