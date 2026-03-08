# Black Forest Labs

- Codigo canonico: `bfl`
- Categoria: `media-audio`
- Capability live nesta fase: `image-generation`, `image-editing`
- Estado: `implemented_with_restrictions`
- Evidencia: `integration_verified`

## Contrato usado

- Docs oficiais: `https://docs.bfl.ai`
- Submit assíncrono via `POST /v1/flux-2-pro` ou `POST /v1/flux-2-klein-4b`
- Polling via `polling_url` retornado pelo provider
- Header de autenticacao: `x-key`

## Observacoes

- A integracao atual usa submit + polling honesto, sem fingir resposta síncrona completa.
- `image editing` nesta fase usa `input_image` remoto; o campo de mask do contrato do Lume nao e exigido para BFL.
- O endpoint interno de polling e `GET /api/v1/images/jobs/{providerCode}/{jobId}` com `pollingUrl` embutido no `pollPath` retornado no submit.
