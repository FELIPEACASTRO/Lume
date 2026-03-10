# ElevenLabs

- Codigo: `elevenlabs`
- Estado: `implemented_with_restrictions`
- Evidencia: `integration_verified`
- Capability live nesta fase: `text-to-speech`
- Endpoint principal no Lume: `POST /api/v1/audio/tts`
- Credencial: `ELEVENLABS_API_KEY`
- Docs oficiais: `https://elevenlabs.io/docs/api-reference/text-to-speech/convert`

## Notas

- O runtime atual converte o audio retornado em `audioBase64`.
- Quando `voice` nao e informado, o service consulta `voices` e usa a primeira voz retornada pela API.
- Voice cloning e fluxos mais avancados continuam fora desta tranche.
