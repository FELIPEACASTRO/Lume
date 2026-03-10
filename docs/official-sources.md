# Official Sources

Este arquivo centraliza as fontes primarias usadas para reconciliar roadmap, catalogo e runtime.

## Regras

- fonte primaria vence material de marketing, blog post, prompt e anotacao local
- se a doc oficial mudar, o catalogo e as matrizes devem ser atualizados na mesma PR
- nenhum provider sobe de `catalog_only` para `live` sem fonte primaria vinculada

## Text runtime

| Provider | Fonte oficial principal |
|---|---|
| OpenAI | `https://developers.openai.com/api/docs/guides/text/` |
| Anthropic | `https://docs.anthropic.com/en/api/messages` |
| Google Gemini | `https://ai.google.dev/api` |
| DeepSeek | `https://api-docs.deepseek.com/api/create-chat-completion/` |
| xAI | `https://docs.x.ai/docs` |
| Perplexity | `https://docs.perplexity.ai/docs/grounded-llm/openai-compatibility` |
| Groq | `https://console.groq.com/docs/openai` |
| OpenRouter | `https://openrouter.ai/docs` |
| Cohere | `https://docs.cohere.com/v2/reference/chat` |
| Together | `https://docs.together.ai/docs/openai-api-compatibility` |
| Fireworks | `https://docs.fireworks.ai/guides/querying-text-models` |
| DeepInfra | `https://deepinfra.com/docs/openai_api` |
| Mistral | `https://docs.mistral.ai/capabilities/completion/` |

## Vector e search

| Provider | Fonte oficial principal |
|---|---|
| Voyage AI | `https://docs.voyageai.com/docs/embeddings` |
| Exa | `https://docs.exa.ai/reference/search` |
| NewsCatcher | `https://www.newscatcherapi.com/docs/v3/api-reference/overview/authentication` |
| Tavily | `https://docs.tavily.com/documentation/api-reference/endpoint/search` |
| SerpApi | `https://serpapi.com/search-api` |

## Enterprise e infra especializada

| Provider | Fonte oficial principal |
|---|---|
| AWS Bedrock | `https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html` |
| Azure OpenAI | `https://learn.microsoft.com/azure/ai-services/openai/reference` |
| Cloudflare Workers AI | `https://developers.cloudflare.com/workers-ai/get-started/rest-api/` |
| GitHub Models | `https://docs.github.com/en/github-models/prototyping-with-ai-models` |
| Hugging Face | `https://huggingface.co/docs/api-inference/index` |
| AI21 | `https://docs.ai21.com/reference` |

## Media, audio e OCR

| Provider | Fonte oficial principal |
|---|---|
| Stability AI | `https://platform.stability.ai/docs` |
| fal.ai | `https://docs.fal.ai` |
| Replicate | `https://replicate.com/docs` |
| Deepgram | `https://developers.deepgram.com` |
| AssemblyAI | `https://www.assemblyai.com/docs` |
| ElevenLabs | `https://elevenlabs.io/docs/api-reference` |
| Google Vision | `https://cloud.google.com/vision/docs` |
| Google Speech-to-Text | `https://cloud.google.com/speech-to-text/docs` |
| Google Text-to-Speech | `https://cloud.google.com/text-to-speech/docs` |
| BFL | `https://docs.bfl.ai` |
| Runway | `https://docs.dev.runwayml.com` |
| Ideogram | `https://developer.ideogram.ai` |

## Threat-intel

| Provider | Fonte oficial principal | Observacao |
|---|---|---|
| DarkOwl | `https://www.darkowl.com/wp-content/uploads/2022/02/API-Welcome-Packet.pdf` | exige contrato e assinatura especifica |
| FullHunt | `https://docs.fullhunt.io/` | onboarding/compliance antes de runtime live |
| Flare | `https://api.docs.flare.io/concepts/authentication` | token transitorio |
| Twingly | `https://www.twingly.com/dark-web-api/` | enterprise/manual |

