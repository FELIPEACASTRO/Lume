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
| Groq | `groq` | - | `responses` | Sim | `groq:llama-3.3-70b-versatile` |
| OpenRouter | `openrouter` | - | `chat-completions` | Sim | `openrouter:openai/gpt-4.1-mini` |
| Together | `together` | - | `chat-completions` | Sim | `together:meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo` |
| Fireworks | `fireworks` | - | `chat-completions` | Sim | `fireworks:accounts/fireworks/models/llama-v3p1-8b-instruct` |
| DeepInfra | `deepinfra` | - | `chat-completions` | Sim | `deepinfra:meta-llama/Meta-Llama-3.1-8B-Instruct` |
| Mistral | `mistral` | - | `chat-completions` | Sim | `mistral:mistral-small-latest` |
| Exa | `exa` | - | `research` | N/A | `exa:search` |
| NewsCatcher | `newscatcher` | - | `research` | N/A | `newscatcher:search` |

Catalogados, mas ainda manuais, skeleton ou fora do runtime capability-aware desta fase:

- `cohere`
- `cloudflare-workers-ai`
- `cerebras`
- `nvidia-nim`
- `sambanova`
- `siliconflow`
- `ai21`
- `azure-openai`
- `github-models`
- `hugging-face`
- `aws-bedrock`
- `google-vision`
- `google-speech-to-text`
- `google-natural-language`
- `google-translation`
- `google-text-to-speech`
- `bfl`
- `runway`
- `ideogram`
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
| Groq | `GROQ_API_KEY` |
| OpenRouter | `OPENROUTER_API_KEY` |
| Together | `TOGETHER_API_KEY` |
| Fireworks | `FIREWORKS_API_KEY` |
| DeepInfra | `DEEPINFRA_API_KEY` |
| Mistral | `MISTRAL_API_KEY` |
| Exa | `EXA_API_KEY` |
| NewsCatcher | `NEWSCATCHER_API_KEY` |

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
- Threat-intel é visível no produto, mas bloqueia execução se `SECURITY_COMPLIANCE_DARK_WEB_ENABLED` estiver desligada ou se a chamada vier sem justificativa.
- Providers fora da lista principal permanecem `catalog-only`, `manual` ou `skeleton` com `TODO official-contract-validation`.
