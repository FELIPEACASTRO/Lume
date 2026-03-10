# AI Providers do Lume

O catalogo exposto pela API agora recebe um overlay de governanca carregado de `provider-governance-metadata.json`.

Campos novos e obrigatorios para leitura honesta do estado do produto:

- `implementationStatus`
- `evidenceLevel`
- `businessPriority`
- `syncMode`
- `pricingSummary`
- `rateLimitSummary`
- `routingModes`
- `documentationSource`

## Escopo real desta fase

Runtime textual realmente ligado:

| Provider | Codigo canonico | Alias | API style | Streaming mode padrao | Modelo padrao |
|---|---|---|---|---|---|
| OpenAI | `openai` | - | `responses` | `unsupported` | `openai:gpt-4.1-mini` |
| Gemini | `google-gemini` | `gemini` | `generate-content` | `unsupported` | `google-gemini:gemini-2.5-flash` |
| DeepSeek | `deepseek` | - | `chat-completions` | `unsupported` | `deepseek:deepseek-chat` |
| Anthropic | `anthropic` | `claude` | `messages` | `unsupported` | `anthropic:claude-sonnet-4-5` |
| xAI | `xai` | `grok` | `responses` | `unsupported` | `xai:grok-4` |
| Perplexity | `perplexity` | - | `chat-completions` | `unsupported` | `perplexity:sonar` |
| Groq | `groq` | - | `responses` | `unsupported` | `groq:llama-3.3-70b-versatile` |
| OpenRouter | `openrouter` | - | `chat-completions` | `unsupported` | `openrouter:openai/gpt-4.1-mini` |
| Cohere | `cohere` | - | `chat-v2` | `unsupported` | `cohere:command-r` |
| Together | `together` | - | `chat-completions` | `unsupported` | `together:meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo` |
| Fireworks | `fireworks` | - | `chat-completions` | `unsupported` | `fireworks:accounts/fireworks/models/llama-v3p1-8b-instruct` |
| DeepInfra | `deepinfra` | - | `chat-completions` | `unsupported` | `deepinfra:meta-llama/Meta-Llama-3.1-8B-Instruct` |
| Mistral | `mistral` | - | `chat-completions` | `unsupported` | `mistral:mistral-small-latest` |

Research live:

| Provider | Codigo | API style | Capability |
|---|---|---|---|
| Exa | `exa` | `research` | `search` |
| NewsCatcher | `newscatcher` | `research` | `search` |
| Tavily | `tavily` | `research` | `search` |
| SerpApi | `serpapi` | `research` | `search` |

Vector runtime live:

| Provider | Codigo | API style | Capability |
|---|---|---|---|
| Voyage AI | `voyage-ai` | `embeddings-rerank` | `embeddings`, `rerank` |
| Cohere | `cohere` | `chat-v2` | `embeddings`, `rerank` |

Media/audio/OCR live com restricoes:

| Provider | Codigo | API style | Capability |
|---|---|---|---|
| Deepgram | `deepgram` | `audio-capability` | `speech-to-text` |
| AssemblyAI | `assemblyai` | `audio-capability` | `speech-to-text` |
| ElevenLabs | `elevenlabs` | `audio-capability` | `text-to-speech` |
| Mistral | `mistral` | `chat-completions` | `ocr` |
| Stability AI | `stability-ai` | `image-capability` | `image-generation` |
| Replicate | `replicate` | `media-job` | `image-generation`, `image-editing`, `video-generation` |
| Ideogram | `ideogram` | `image-capability` | `image-generation`, `image-editing` |
| Black Forest Labs | `bfl` | `image-job` | `image-generation`, `image-editing` |
| Runway | `runway` | `video-job` | `video-generation` |

Catalogados/manual/skeleton nesta rodada:

- `cloudflare-workers-ai`
- `azure-openai`
- `aws-bedrock`
- `hugging-face`
- `ai21`
- `cerebras`
- `nvidia-nim`
- `sambanova`
- `siliconflow`
- `github-models`
- media/audio providers ainda fora desta rodada, exceto `deepgram`, `assemblyai`, `elevenlabs`, `mistral` OCR, `stability-ai`, `replicate`, `ideogram`, `bfl` e `runway`
- threat-intel providers sem adapter live

## Capability API realmente ligada

- `POST /api/v1/embeddings`
  - `cohere`
  - `voyage-ai`
- `POST /api/v1/rerank`
  - `cohere`
  - `voyage-ai`
- `POST /api/v1/search`
  - `exa`
  - `newscatcher`
  - `tavily`
  - `serpapi`
- `POST /api/v1/audio/stt`
  - `deepgram`
  - `assemblyai`
- `POST /api/v1/audio/tts`
  - `elevenlabs`
- `POST /api/v1/ocr`
  - `mistral`
- `POST /api/v1/images/generate`
  - `stability-ai`
  - `replicate`
  - `ideogram`
  - `bfl`
- `POST /api/v1/images/edit`
  - `replicate`
  - `ideogram`
  - `bfl`
- `GET /api/v1/images/jobs/{provider}/{jobId}`
  - `replicate`
  - `bfl`
- `POST /api/v1/videos/generate`
  - `replicate`
  - `runway`
- `GET /api/v1/videos/jobs/{provider}/{jobId}`
  - `replicate`
  - `runway`

## Contratos e metadados importantes

### Provider DTO

Os endpoints de provider agora expõem:

- `streamingMode = native | unsupported`
- `runtimeMaturity = live | partial | catalog_only`
- `catalogState`
- `credentialFields[]`

### Health DTO

`GET /api/v1/providers/health` agora diferencia:

- `healthSource = static | last_connectivity_test`
- `snapshotPersistence = memory | durable`

Hoje o snapshot de conectividade e:

- `snapshotPersistence = memory`

Ou seja:

- ele e util como contexto operacional recente
- ele nao deve ser tratado como evidencia duravel ou readiness forte

## Endpoints relevantes

- `POST /api/v1/inference/execute`
- `POST /api/v1/inference/stream`
- `POST /api/v1/chat`
- `POST /api/v1/responses`
- `POST /api/v1/search`
- `POST /api/v1/web-grounded-chat`
- `POST /api/v1/threat-intel/search`
- `GET /api/v1/providers`
- `GET /api/v1/providers/{provider}`
- `GET /api/v1/providers/{provider}/models`
- `GET /api/v1/providers/status`
- `GET /api/v1/providers/health`
- `POST /api/v1/providers/{code}/connectivity-test`

## Contrato de inferencia

Exemplo:

```json
{
  "providerCode": "openai",
  "modelCode": "openai:gpt-4.1-mini",
  "systemPrompt": "Seja direto.",
  "prompt": "Resuma o status do workspace.",
  "fallbackProviderCodes": ["anthropic", "google-gemini"],
  "temperature": 0.2,
  "maxTokens": 300
}
```

Regras:

- nao existe fallback implicito
- a ordem de `fallbackProviderCodes` e respeitada
- cada tentativa gera metricas, latencia e status proprios

## Observabilidade

Metricas registradas:

- `lume.ai.request.latency`
- `lume.ai.request.success`
- `lume.ai.request.error`
- `lume.ai.request.timeout`
- `lume.ai.request.fallback`
- `lume.ai.request.estimated_cost_usd`

Tags padronizadas:

- `provider`
- `model`
- `operation`
- `status`

Observacao:

- `streaming_mode` ainda nao esta propagado como tag em todas as metricas; isso segue como refinamento tecnico pendente.

## Seguranca

- segredos lidos por `SecretResolver`
- `Environment` nao deve ser acessado fora do resolvedor
- `.env` fora do git
- headers sensiveis mascarados
- prompts nao devem ir para log em claro
- threat-intel exige compliance flag e justificativa por chamada

## Threat-intel

Threat-intel continua:

- `admin-only`
- opt-in
- bloqueado quando `SECURITY_COMPLIANCE_DARK_WEB_ENABLED` estiver desligado
- dependente de justificativa por chamada

Mesmo com visibilidade no produto, isso nao significa runtime live para todos os providers de threat-intel.
