# Live Validation Matrix

Esta matriz separa claramente o que esta:

- modelado no catalogo
- validado localmente
- validado online

## Estado atual

| Grupo | Providers | Estado alvo | Evidencia atual |
|---|---|---|---|
| text-runtime principal | OpenAI, Anthropic, Gemini, DeepSeek, xAI, Cohere | `online_verified` quando houver credenciais | `integration_verified` no repo |
| text-runtime agregado | Perplexity, Groq, OpenRouter, Together, Fireworks, DeepInfra, Mistral, DashScope / Qwen, SiliconFlow | `online_verified` quando houver credenciais | `integration_verified` no repo |
| vector-runtime | Voyage AI, Cohere, DashScope / Qwen, SiliconFlow | `online_verified` quando houver credenciais | `integration_verified` no repo |
| search | Exa, NewsCatcher, Tavily, SerpApi | `online_verified` quando houver credenciais | `integration_verified` no repo |
| media/audio | Stability, fal.ai, Replicate, Deepgram, AssemblyAI, ElevenLabs, Google Cloud, BFL, Runway, Ideogram | `online_verified` quando houver credenciais | `integration_verified` para Stability AI, fal.ai, Replicate, Deepgram, AssemblyAI, ElevenLabs, Google Vision/STT/TTS/Translation/NLP, Mistral OCR, Ideogram, BFL e Runway |
| enterprise | Bedrock, Azure OpenAI, Cloudflare Workers AI, GitHub Models, Hugging Face, AI21, Cerebras, NVIDIA NIM, SambaNova, SiliconFlow | `online_verified` quando houver credenciais | `integration_verified` para Bedrock, Azure OpenAI, Cloudflare Workers AI, Hugging Face, AI21, Cerebras, NVIDIA NIM, SambaNova e SiliconFlow; `catalog_only` para GitHub Models |
| threat-intel | FullHunt, Flare, DarkOwl, Twingly, Onion Search Engine e DarknetSearch | `online_verified` quando houver credenciais e compliance | `integration_verified` para FullHunt, Flare, DarkOwl, Twingly, Onion Search Engine e DarknetSearch |

## Politica

- `online_verified` so pode ser atribuido apos smoke live opt-in
- ausencia de credencial nao e falha do codigo; e blocker operacional
- provider enterprise sem tenant/contrato continua `blocked` ou `catalog_only`
- DashScope / Qwen so pode subir para `online_verified` com smoke real na regiao Beijing ou evidencia equivalente de gratuidade recorrente
