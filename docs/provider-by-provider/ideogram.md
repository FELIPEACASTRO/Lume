# Ideogram

## Estado no Lume

- codigo: `ideogram`
- categoria: `media-audio`
- implementation status: `implemented_with_restrictions`
- evidence level: `integration_verified`
- runtime atual:
  - `POST /api/v1/images/generate`
  - `POST /api/v1/images/edit`

## O que esta ligado

- geracao sincrona via `v1/ideogram-v3/generate`
- edicao sincrona via `v1/ideogram-v3/edit`
- assets retornados como `assetUrls`

## Restricoes atuais

- a edicao atual exige `inputImageUrl` e `maskImageUrl`
- o runtime atual trabalha com download remoto dos arquivos antes de montar o multipart
- ainda nao ha persistencia propria dos assets no storage do Lume

## Credenciais

- `IDEOGRAM_API_KEY`

## Fonte primaria

- `https://developer.ideogram.ai`
