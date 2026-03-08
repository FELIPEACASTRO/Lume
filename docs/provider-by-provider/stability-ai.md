# Stability AI

- codigo: `stability-ai`
- categoria: `media-audio`
- capability live nesta fase: `image-generation`
- estado: `implemented_with_restrictions`
- evidencia: `integration_verified`

## Escopo real

- `Stable Image Core` ligado em `POST /api/v1/images/generate`
- retorno atual normalizado em `assetBase64`
- `image editing`, `video` e `audio` permanecem fora desta fase

## Credenciais

- `STABILITY_API_KEY`

## Fonte oficial

- `https://platform.stability.ai/docs`

## Observacoes

- a integracao usa `REST v2beta`
- a chamada e sincrona e nao cria job interno
- o runtime nao declara `online_verified` sem smoke live opt-in com credenciais reais
