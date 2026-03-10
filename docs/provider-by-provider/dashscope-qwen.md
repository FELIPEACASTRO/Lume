# DashScope / Qwen

- estado: `implemented_with_restrictions`
- evidencia: `integration_verified`
- capabilities: `chat`, `responses`, `embeddings`, `rerank`
- auth: `DASHSCOPE_API_KEY`
- api principal: DashScope/Bailian em modo compativel OpenAI para chat e endpoints dedicados para vector/rerank
- free tier: recorrente apenas para modelos elegiveis na regiao China continental (Beijing)
- free models:
  - `qwen-plus`
  - `text-embedding-v4`
  - `gte-rerank-v2`
- restricao regional:
  - `cn-beijing`
- observacao: o provider exige cuidado operacional porque a regiao Singapore e outras podem gerar cobranca mesmo quando a oferta gratuita existe em Beijing
