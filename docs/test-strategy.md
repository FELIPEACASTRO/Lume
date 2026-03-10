# Test Strategy

## Niveis de evidencia

- `offline_verified`: logica pura, parser, mapper, validator, roteamento, redaction
- `integration_verified`: adapters e APIs com contratos controlados
- `online_verified`: smoke live opt-in com credenciais e contrato real

## Piramide de testes

- unit: services, mappers, metadata loaders, routing, retry, redaction
- contract: request/response shape por provider e por capability
- integration: controllers versionados, catalogo, health, connectivity e services capability-aware
- live smoke: apenas com credenciais e flag habilitada

## Suites obrigatorias por capability

| Capability | Minimo obrigatorio |
|---|---|
| `chat/responses` | unit + integration + live smoke opt-in |
| `embeddings/rerank` | unit + integration |
| `search/web-grounded-chat` | unit + integration |
| `audio/ocr` | unit + integration + artifact lifecycle |
| `image/video` | unit + integration + polling |
| `threat-intel` | unit + integration + compliance/security |

## Gates

- backend: `mvn verify`
- docs: matrizes e README atualizados
- nenhum provider `online_verified` sem evidencia live real
