# Provider Status Matrix

Esta matriz e a visao humana dos mesmos estados expostos pelo catalogo e pelo overlay de `provider-governance-metadata.json`.

## Text runtime

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| OpenAI | `openai` | `live` | `integration_verified` | `high_roi` | principal referencia para `responses` |
| Anthropic | `anthropic` | `live` | `integration_verified` | `high_roi` | runtime textual real |
| Google Gemini | `google-gemini` | `live` | `integration_verified` | `high_roi` | `generateContent` nativo |
| DeepSeek | `deepseek` | `live` | `integration_verified` | `medium_roi` | observar trial/tiers fora do core |
| xAI | `xai` | `live` | `integration_verified` | `medium_roi` | responses real |
| Perplexity | `perplexity` | `implemented_with_restrictions` | `integration_verified` | `high_roi` | grounded/citations ainda amadurecendo |
| Groq | `groq` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | forte para latencia |
| Mistral | `mistral` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat live; OCR capability tambem ligada |
| OpenRouter | `openrouter` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | free tier depende de `:free` |
| Cohere | `cohere` | `live` | `integration_verified` | `high_roi` | chat + vector real |
| Together AI | `together` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | textual live |
| Fireworks AI | `fireworks` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | textual live |
| DeepInfra | `deepinfra` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | textual live |
| Cloudflare Workers AI | `cloudflare-workers-ai` | `catalog_only` | `offline_verified` | `contract_dependent` | requer adapter dedicado |
| Cerebras | `cerebras` | `catalog_only` | `offline_verified` | `medium_roi` | catalogado |
| NVIDIA NIM | `nvidia-nim` | `catalog_only` | `offline_verified` | `medium_roi` | catalogado |
| SambaNova | `sambanova` | `catalog_only` | `offline_verified` | `medium_roi` | catalogado |
| SiliconFlow | `siliconflow` | `catalog_only` | `offline_verified` | `medium_roi` | catalogado |
| AWS Bedrock | `aws-bedrock` | `catalog_only` | `offline_verified` | `contract_dependent` | depende de gateway enterprise |
| Hugging Face | `hugging-face` | `catalog_only` | `offline_verified` | `medium_roi` | task/model resolution futura |
| AI21 | `ai21` | `catalog_only` | `offline_verified` | `medium_roi` | catalogado |
| GitHub Models | `github-models` | `catalog_only` | `offline_verified` | `medium_roi` | depende de PAT/scope/plano |

## Research e vector

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Exa | `exa` | `live` | `integration_verified` | `high_roi` | research real |
| NewsCatcher | `newscatcher` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | focado em news search |
| Tavily | `tavily` | `live` | `integration_verified` | `high_roi` | alto ROI para search |
| SerpApi | `serpapi` | `live` | `integration_verified` | `medium_roi` | SERP generalista |
| Voyage AI | `voyage-ai` | `live` | `integration_verified` | `high_roi` | embeddings e rerank reais |

## Media e audio

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Stability AI | `stability-ai` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | Stable Image Core ligado para image generation sync com retorno em base64 |
| fal.ai | `fal-ai` | `catalog_only` | `offline_verified` | `medium_roi` | jobs assincronos |
| Replicate | `replicate` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image generation, image editing e video generation via predictions assincronas |
| Deepgram | `deepgram` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | STT live via capability API |
| AssemblyAI | `assemblyai` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | STT live via submit + polling curto |
| ElevenLabs | `elevenlabs` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | TTS live via capability API |
| Google Vision | `google-vision` | `catalog_only` | `offline_verified` | `medium_roi` | OCR/vision futuro |
| Google Speech-to-Text | `google-speech-to-text` | `catalog_only` | `offline_verified` | `medium_roi` | STT futuro |
| Google Natural Language | `google-natural-language` | `catalog_only` | `offline_verified` | `medium_roi` | NLP especializada |
| Google Translation | `google-translation` | `catalog_only` | `offline_verified` | `medium_roi` | traducao especializada |
| Google Text-to-Speech | `google-text-to-speech` | `catalog_only` | `offline_verified` | `medium_roi` | TTS futura |
| Black Forest Labs | `bfl` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | FLUX 2 para image generation e editing via submit + polling |
| Runway | `runway` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image-to-video assincrono com submit + poll interno |
| Ideogram | `ideogram` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image generation e edit v3 sincronas |

## Enterprise gateways e threat-intel

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Azure OpenAI | `azure-openai` | `catalog_only` | `offline_verified` | `contract_dependent` | gateway enterprise |
| DarkOwl | `darkowl` | `blocked` | `offline_verified` | `contract_dependent` | exige compliance, contrato e HMAC |
| Onion Search Engine | `onion-search-engine` | `blocked` | `offline_verified` | `contract_dependent` | docs publicas insuficientes |
| Twingly | `twingly` | `blocked` | `offline_verified` | `contract_dependent` | dark web API enterprise |
| FullHunt | `fullhunt` | `blocked` | `offline_verified` | `contract_dependent` | exige onboarding/compliance |
| DarknetSearch | `darknetsearch` | `blocked` | `offline_verified` | `contract_dependent` | docs insuficientes |
| Flare | `flare` | `blocked` | `offline_verified` | `contract_dependent` | exige token temporario e compliance |
