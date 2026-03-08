# Mistral

- Codigo: `mistral`
- Estado: `implemented_with_restrictions`
- Evidencia: `integration_verified`
- Capabilities live nesta fase: `chat`, `responses` compativeis via runtime textual e `ocr`
- Endpoint principal de OCR no Lume: `POST /api/v1/ocr`
- Credencial: `MISTRAL_API_KEY`
- Docs oficiais:
  - `https://docs.mistral.ai/capabilities/completion/`
  - `https://docs.mistral.ai/capabilities/document/`

## Notas

- O runtime textual continua usando o modelo padrao `mistral:mistral-small-latest`.
- OCR usa o modelo `mistral:mistral-ocr-latest`.
- Google Vision e outros providers de OCR seguem fora desta tranche.
