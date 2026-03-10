# Requirements Matrix

## Fonte de verdade e precedencia

Ao reconciliar conflito entre prompt, backlog, README e runtime, a precedencia no Lume passa a ser:

1. codigo executavel + testes verdes no repo
2. `backend/src/main/resources/provider-governance-metadata.json`
3. `docs/ai-providers.md` e `README.md`
4. artefatos de descoberta do `Downloads` como `prompt-mestre-codex.txt` e catalogos auxiliares
5. documentacao oficial do fornecedor para fechar lacunas contratuais ou de autenticacao

Regra operacional:

- nenhum provider deve ser declarado `live` sem evidencia no codigo e sem contrato visivel na matriz
- nenhum provider deve ser tratado como completo se estiver sem credenciais, sem smoke live ou sem docs primarias suficientes
- `threat-intel` permanece opt-in, `admin-only` e dependente de `SECURITY_COMPLIANCE_DARK_WEB_ENABLED=true`

## Taxonomia oficial de estado

| Campo | Valores | Uso |
|---|---|---|
| `implementationStatus` | `live`, `implemented_with_restrictions`, `catalog_only`, `blocked`, `out_of_scope` | estado de entrega do provider no produto |
| `runtimeMaturity` | `live`, `partial`, `catalog_only` | maturidade do runtime tecnico na API |
| `evidenceLevel` | `offline_verified`, `integration_verified`, `online_verified` | nivel de evidencia disponivel |
| `streamingMode` | `native`, `unsupported` | semantica real de streaming |

## Recorte curado de valor

| Classe | O que entra |
|---|---|
| `core_now` | workspace/tenant/member/role, projects/tasks/library/inbox/usage, budgets por workspace, providers capability-first, search/RAG inicial, audit/compliance basicos e API capability-first |
| `next_after_core` | knowledge plane completo, prompt/template library, artifact versioning, BYOK por tenant, virtual API keys, workflow runtime, approvals e semantic cache |
| `parked` | marketplace publico, revenue share, white-label amplo, browser/computer use, telefonia e claims de escala sem benchmark |

## Matriz por capability

| Capability | Endpoint principal | Estado atual | Providers principais | Evidencia minima para `live` |
|---|---|---|---|---|
| `CHAT` | `POST /api/v1/chat` | ligado | OpenAI, Anthropic, Gemini, DeepSeek, xAI, Perplexity, Groq, OpenRouter, Cohere, Together, Fireworks, DeepInfra, Mistral | adapter dedicado, contrato de request/response, connectivity test |
| `RESPONSES` | `POST /api/v1/responses` | ligado | OpenAI, xAI, Groq e demais via gateway textual | adapter dedicado, cobertura de erro e timeout |
| `EMBEDDINGS` | `POST /api/v1/embeddings` | ligado | Cohere, Voyage AI | payload normalizado, uso/custo expostos |
| `RERANK` | `POST /api/v1/rerank` | ligado | Cohere, Voyage AI | contrato real, retorno ordenado e custo |
| `WEB_SEARCH` | `POST /api/v1/search` | ligado | Exa, NewsCatcher, Tavily, SerpApi | pesquisa real, links/snippets, connectivity test |
| `WEB_GROUNDED_CHAT` | `POST /api/v1/web-grounded-chat` | parcial | Perplexity e combinacao explicita de provider textual + provider de research | citations/grounding reais antes de declarar `live` |
| `IMAGE_GENERATION` | `POST /api/v1/images/generate` | `partial` | Stability AI sync, Replicate async, Ideogram sync e BFL async live com restricoes | adapter capability-aware + asset normalization |
| `IMAGE_EDITING` | `POST /api/v1/images/edit` | `partial` | Replicate async, Ideogram sync e BFL async live com restricoes | adapter capability-aware + mask/asset normalization |
| `VIDEO_GENERATION` | `POST /api/v1/videos/generate` | `partial` | Replicate e Runway live com restricoes | submit/poll/result real com job lifecycle interno |
| `SPEECH_TO_TEXT` | `POST /api/v1/audio/stt` | `partial` | Deepgram e AssemblyAI live; Google STT segue em roadmap | adapter real + artifacts |
| `TEXT_TO_SPEECH` | `POST /api/v1/audio/tts` | `partial` | ElevenLabs live; Deepgram/Google TTS seguem em roadmap | adapter real + artifacts |
| `OCR` | `POST /api/v1/ocr` | `partial` | Mistral OCR live; Google Vision e correlatos seguem em roadmap | adapter real + metadata de pagina |
| `THREAT_INTEL_SEARCH` | `POST /api/v1/threat-intel/search` | gated | DarkOwl, Flare, FullHunt, Twingly e correlatos | compliance flag, RBAC, auditoria, justificativa, redaction |

## Matriz de aceite por evidencia

| Evidence level | O que significa | O que nao significa |
|---|---|---|
| `offline_verified` | docs analisadas, metadata catalogada e contrato modelado | provider pronto para producao |
| `integration_verified` | testes unitarios/contrato/integracao controlada no repo | smoke online com credenciais reais |
| `online_verified` | smoke live opt-in executado com credenciais validas | cobertura completa de carga ou compliance enterprise |

## Gates de release

| Gate | Obrigatorio |
|---|---|
| build backend | `mvn verify` |
| build frontend | `pnpm lint`, `pnpm test`, `pnpm build` |
| docs | matrizes atualizadas e README coerente com o estado real |
| provider catalog | nenhum provider marcado como `live` sem `implementationStatus` e `evidenceLevel` coerentes |
| threat-intel | bloqueado sem permissao, compliance flag e justificativa |
