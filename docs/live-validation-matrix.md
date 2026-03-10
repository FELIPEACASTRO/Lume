# Live Validation Matrix

Esta matriz separa claramente o que esta:

- modelado no catalogo
- validado localmente
- validado online

## Estado atual

| Grupo | Providers | Estado alvo | Evidencia atual |
|---|---|---|---|
| text-runtime principal | OpenAI, Anthropic, Gemini, DeepSeek, xAI, Cohere | `online_verified` quando houver credenciais | `integration_verified` no repo |
| text-runtime agregado | Perplexity, Groq, OpenRouter, Together, Fireworks, DeepInfra, Mistral | `online_verified` quando houver credenciais | `integration_verified` no repo |
| vector-runtime | Voyage AI, Cohere | `online_verified` quando houver credenciais | `integration_verified` no repo |
| search | Exa, NewsCatcher, Tavily, SerpApi | `online_verified` quando houver credenciais | `integration_verified` no repo |
| media/audio | Stability, fal.ai, Replicate, Deepgram, AssemblyAI, ElevenLabs, Google Cloud, BFL, Runway, Ideogram | `online_verified` quando houver credenciais | `integration_verified` para Stability AI, Replicate, Deepgram, AssemblyAI, ElevenLabs, Mistral OCR, Ideogram, BFL e Runway; restante `offline_verified` |
| enterprise | Bedrock, Azure OpenAI, Cloudflare Workers AI, GitHub Models, Hugging Face, AI21 | futura | `offline_verified` ou `catalog_only` |
| threat-intel | FullHunt, Flare, DarkOwl, Twingly e correlatos | futura | `blocked` ou `offline_verified` |

## Politica

- `online_verified` so pode ser atribuido apos smoke live opt-in
- ausencia de credencial nao e falha do codigo; e blocker operacional
- provider enterprise sem tenant/contrato continua `blocked` ou `catalog_only`
