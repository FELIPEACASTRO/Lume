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
| DashScope / Qwen | `dashscope-qwen` | `implemented_with_restrictions` | `integration_verified` | `high_roi` | chat e camada vetorial com gratuidade recorrente restrita a Beijing |
| Cloudflare Workers AI | `cloudflare-workers-ai` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | chat inicial via compatibilidade OpenAI |
| Cerebras | `cerebras` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat inicial OpenAI-compatible |
| NVIDIA NIM | `nvidia-nim` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat inicial OpenAI-compatible |
| SambaNova | `sambanova` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat inicial OpenAI-compatible |
| SiliconFlow | `siliconflow` | `implemented_with_restrictions` | `integration_verified` | `high_roi` | chat, embeddings e rerank com modelos gratuitos selecionados |
| AWS Bedrock | `aws-bedrock` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | runtime via AWS SDK, sujeito a smoke por conta/regiao |
| Hugging Face | `hugging-face` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat via router e embeddings via Inference API |
| AI21 | `ai21` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | chat inicial para Jamba |
| GitHub Models | `github-models` | `catalog_only` | `offline_verified` | `medium_roi` | depende de PAT/scope/plano |

## Research e vector

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Exa | `exa` | `live` | `integration_verified` | `high_roi` | research real |
| NewsCatcher | `newscatcher` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | focado em news search |
| Tavily | `tavily` | `live` | `integration_verified` | `high_roi` | alto ROI para search |
| SerpApi | `serpapi` | `live` | `integration_verified` | `medium_roi` | SERP generalista |
| Voyage AI | `voyage-ai` | `live` | `integration_verified` | `high_roi` | embeddings e rerank reais |
| DashScope / Qwen | `dashscope-qwen` | `implemented_with_restrictions` | `integration_verified` | `high_roi` | embeddings e rerank com restricao regional de gratuidade |
| SiliconFlow | `siliconflow` | `implemented_with_restrictions` | `integration_verified` | `high_roi` | embeddings e rerank em modelos gratuitos selecionados |

## Media e audio

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Stability AI | `stability-ai` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | Stable Image Core ligado para image generation sync com retorno em base64 |
| fal.ai | `fal-ai` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | jobs assincronos reais para imagem e video |
| Replicate | `replicate` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image generation, image editing e video generation via predictions assincronas |
| Deepgram | `deepgram` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | STT live via capability API |
| AssemblyAI | `assemblyai` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | STT live via submit + polling curto |
| ElevenLabs | `elevenlabs` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | TTS live via capability API |
| Google Vision | `google-vision` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | OCR inicial via Vision annotate |
| Google Speech-to-Text | `google-speech-to-text` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | STT sincrono inicial via REST |
| Google Natural Language | `google-natural-language` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | entities, sentiment e classification |
| Google Translation | `google-translation` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | traducao de texto via endpoint dedicado |
| Google Text-to-Speech | `google-text-to-speech` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | sintese inicial por REST |
| Black Forest Labs | `bfl` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | FLUX 2 para image generation e editing via submit + polling |
| Runway | `runway` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image-to-video assincrono com submit + poll interno |
| Ideogram | `ideogram` | `implemented_with_restrictions` | `integration_verified` | `medium_roi` | image generation e edit v3 sincronas |

## Enterprise gateways e threat-intel

| Provider | Codigo | Estado | Evidencia | Prioridade | Observacao |
|---|---|---|---|---|---|
| Azure OpenAI | `azure-openai` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | gateway enterprise com deployment e api-version |
| DarkOwl | `darkowl` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | exige compliance, contrato e HMAC |
| Onion Search Engine | `onion-search-engine` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | busca inicial via API oficial `api.php`, sempre sob compliance |
| Twingly | `twingly` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | dark web search API com header `Authorization: apikey ...`, sempre sob compliance |
| FullHunt | `fullhunt` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | busca inicial de credenciais comprometidas |
| DarknetSearch | `darknetsearch` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | busca inicial via OAuth password flow e leak extended search |
| Flare | `flare` | `implemented_with_restrictions` | `integration_verified` | `contract_dependent` | busca com token temporario e compliance |
