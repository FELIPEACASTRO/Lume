# AI Providers do Lume

## Escopo desta fase

Runtime real habilitado:

| Provider | Código canônico | Alias aceito | API style | Streaming | Modelo padrão |
|---|---|---|---|---|---|
| OpenAI | `openai` | - | `responses` | Sim | `openai:gpt-4.1-mini` |
| Gemini | `google-gemini` | `gemini` | `generate-content` | Sim | `google-gemini:gemini-2.5-flash` |
| DeepSeek | `deepseek` | - | `chat-completions` | Sim | `deepseek:deepseek-chat` |
| Anthropic | `anthropic` | `claude` | `messages` | Sim | `anthropic:claude-sonnet-4-5` |
| xAI | `xai` | `grok` | `responses` | Sim | `xai:grok-4` |
| Perplexity | `perplexity` | - | `chat-completions` | Sim | `perplexity:sonar` |

Fora do runtime real nesta fase:

- `groq`
- `mistral`
- `openrouter`
- `cohere`
- `cloudflare-workers-ai`
- `together`
- `fireworks`
- `deepinfra`
- demais providers de `research-search`, `media-audio` e `threat-intel`

## Variáveis de ambiente obrigatórias

| Provider | Env var |
|---|---|
| OpenAI | `OPENAI_API_KEY` |
| Gemini | `GEMINI_API_KEY` |
| DeepSeek | `DEEPSEEK_API_KEY` |
| Anthropic | `ANTHROPIC_API_KEY` |
| xAI | `XAI_API_KEY` |
| Perplexity | `PERPLEXITY_API_KEY` |

## Endpoints relevantes

- `POST /api/v1/inference/execute`
- `POST /api/v1/inference/stream`
- `GET /api/v1/providers`
- `GET /api/v1/providers/status`
- `GET /api/v1/providers/health`
- `POST /api/v1/providers/{code}/connectivity-test`

## Contrato de inferência

Payload principal:

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

Sem fallback implícito:

- se `fallbackProviderCodes` estiver vazio, apenas o provider pedido é usado
- se houver fallback, a ordem é respeitada exatamente como enviada

## Observabilidade

Métricas:

- `lume.ai.request.latency`
- `lume.ai.request.success`
- `lume.ai.request.error`
- `lume.ai.request.timeout`
- `lume.ai.request.fallback`
- `lume.ai.request.estimated_cost_usd`

Tags:

- `provider`
- `model`
- `operation`
- `status`

## Segurança

- Segredos lidos apenas por env vars.
- `.env` permanece fora do git.
- Logs mascaram headers sensíveis.
- Prompts não são logados em claro.
- Startup valida readiness sem expor valores.

## Limites desta fase

- O endpoint agregado de health não faz probe externo caro por padrão.
- `estimateCost` é heurístico e depende da configuração opcional de pricing em `application.yml`.
- Providers fora da lista principal permanecem `catalog-only` ou `manual`.
