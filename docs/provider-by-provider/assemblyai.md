# AssemblyAI

## Estado no Lume

- codigo: `assemblyai`
- categoria: `media-audio`
- implementation status: `implemented_with_restrictions`
- evidence level: `integration_verified`
- runtime atual:
  - `POST /api/v1/audio/stt`

## O que esta ligado

- speech-to-text via submit em `/v2/transcript`
- polling curto no mesmo fluxo para tentar concluir a transcricao
- retorno honesto de `queued` ou `processing` quando o provider ainda nao concluiu

## Restricoes atuais

- o runtime atual usa `audioUrl` remoto
- audio intelligence avancado permanece fora desta fase
- nao ha job handle proprio para STT nesta capability; o endpoint responde de forma sincrona/curta

## Credenciais

- `ASSEMBLYAI_API_KEY`

## Fonte primaria

- `https://www.assemblyai.com/docs`
