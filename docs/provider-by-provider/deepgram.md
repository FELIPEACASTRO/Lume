# Deepgram

- Codigo: `deepgram`
- Estado: `implemented_with_restrictions`
- Evidencia: `integration_verified`
- Capability live nesta fase: `speech-to-text`
- Endpoint principal no Lume: `POST /api/v1/audio/stt`
- Credencial: `DEEPGRAM_API_KEY`
- Docs oficiais: `https://developers.deepgram.com/docs/speech-to-text`

## Notas

- O runtime atual usa audio remoto via `audioUrl`.
- `text-to-speech` do proprio Deepgram segue fora desta tranche.
- Connectivity test usa um audio de referencia publico da propria Deepgram quando a credencial existe.
