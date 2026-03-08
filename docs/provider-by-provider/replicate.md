# Replicate

- Codigo: `replicate`
- Categoria: `media-audio`
- Estado atual: `implemented_with_restrictions`
- Evidencia: `integration_verified`
- API key: `REPLICATE_API_TOKEN`
- Portal: `https://replicate.com/account/api-tokens`
- Docs oficiais: `https://replicate.com/docs`

## Capabilities ligadas nesta fase

- `image-generation`
  - modelo default: `replicate:black-forest-labs/flux-2-dev`
- `image-editing`
  - modelo default: `replicate:black-forest-labs/flux-kontext-dev`
- `video-generation`
  - modelo default: `replicate:xai/grok-imagine-video`

## Modo de execucao

- submit + polling via Predictions API
- `GET /api/v1/images/jobs/{providerCode}/{jobId}` para imagem
- `GET /api/v1/videos/jobs/{providerCode}/{jobId}` para video

## Restricoes desta tranche

- o runtime atual cobre apenas os modelos oficiais acima
- audio e outros modelos do catalogo Replicate continuam fora desta fase
- `online_verified` continua dependente de smoke live opt-in com credenciais reais
