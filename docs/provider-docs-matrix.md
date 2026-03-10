# Provider Docs Matrix

Esta matriz nao replica todas as URLs do catalogo. O source-of-truth dos links continua sendo:

- `backend/src/main/java/com/lume/workspace/service/ProviderCatalogService.java`
- `backend/src/main/resources/provider-governance-metadata.json`
- `GET /api/v1/providers`

O objetivo desta matriz e registrar **proveniencia documental**, modo de operacao e risco de onboarding.

## Familias e origem documental

| Familia | Providers | Fonte documental dominante | Sync mode dominante | Observacao operacional |
|---|---|---|---|---|
| `text-runtime` | OpenAI, Anthropic, Gemini, DeepSeek, xAI, Perplexity, Groq, OpenRouter, Cohere, Together, Fireworks, DeepInfra, Mistral, DashScope / Qwen, SiliconFlow | `primary_docs` | `sync` | catalogo usa docs oficiais e runtime ja ligado para a maioria |
| `research-search` | Exa, NewsCatcher, Tavily, SerpApi | `primary_docs` | `sync` | providers com alto ROI para grounding e busca |
| `vector-runtime` | Cohere, Voyage AI, DashScope / Qwen, SiliconFlow | `primary_docs` | `sync` | embeddings e rerank devem continuar capability-first |
| `media-audio` | Stability, fal.ai, Replicate, Deepgram, AssemblyAI, ElevenLabs, Google Cloud media/audio, BFL, Runway, Ideogram | `primary_docs` | `both` | familia mais sensivel a jobs assincronos e custo de artifacts |
| `enterprise-gateway` | Azure OpenAI, AWS Bedrock, Cloudflare Workers AI | `primary_docs` | `sync` | onboarding depende de tenant, conta, deployment ou contrato |
| `threat-intel` | DarkOwl, Onion Search Engine, Twingly, FullHunt, DarknetSearch, Flare | `mixed_sources` | `manual` | exige compliance, justificativa, auditoria e redaction |

## Politica de uso de links

| Caso | Regra |
|---|---|
| docs oficiais publicas | manter em `ProviderCatalogService` e no catalogo exposto pela API |
| docs enterprise/portal | manter no catalogo, mas nunca declarar `live` sem contrato e smoke live |
| docs insuficientes ou marketing-only | marcar provider como `blocked` ou `catalog_only`; nunca promover para runtime real |

## Portais de credencial e onboarding

| Grupo | Politica |
|---|---|
| self-service | OpenAI, Anthropic, Gemini, xAI, Perplexity, Groq, OpenRouter, Cohere, Together, Fireworks, DeepInfra, Mistral, Voyage, Tavily, SerpApi, SiliconFlow |
| self-service com ressalvas | DeepSeek, NewsCatcher, Exa, DashScope / Qwen (gratuidade restrita a Beijing) |
| enterprise/manual | Azure OpenAI, Bedrock, Cloudflare Workers AI, Twingly, DarkOwl, Flare, FullHunt |
| implemented_with_restrictions sob compliance | Twingly, DarkOwl, Flare, FullHunt, Onion Search Engine e DarknetSearch |

## Regras de reconciliacao

1. se a URL do provider no catalogo divergir da documentacao viva, corrigir o catalogo e a matriz na mesma PR
2. se o provider mudar de `primary_docs` para `mixed_sources`, rebaixar a evidencia antes de anunciar suporte
3. `threat-intel` so pode usar links e portais que passem por revisao de compliance
