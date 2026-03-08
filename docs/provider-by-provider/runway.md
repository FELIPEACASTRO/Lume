# Runway

## Estado no Lume

- codigo: `runway`
- categoria: `media-audio`
- implementation status: `implemented_with_restrictions`
- evidence level: `integration_verified`
- runtime atual:
  - `POST /api/v1/videos/generate`
  - `GET /api/v1/videos/jobs/{providerCode}/{jobId}`

## O que esta ligado

- submit assincrono de `image_to_video`
- polling interno do job via endpoint versionado do Lume
- retorno de `AsyncJobHandle` com `pollPath` interno

## Restricoes atuais

- o fluxo suportado nesta fase exige `inputImageUrl`
- text-to-video puro continua fora desta tranche por ambiguidade contratual entre modos e modelos
- os assets retornados pelo provider ainda nao sao re-hospedados pelo Lume

## Credenciais

- `RUNWAY_API_KEY`

## Fonte primaria

- `https://docs.dev.runwayml.com`
