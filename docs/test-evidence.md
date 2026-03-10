# Test Evidence

## Evidencia mantida no repo

- relatorios de cobertura backend em `backend/target/site/jacoco/`
- relatórios derivados gerados em `reports/`

## Evidencia da rodada atual

Comandos executados nesta rodada:

- `backend`: `mvn -q test`
- `backend`: `mvn -q verify`
- `docker compose config --no-interpolate`

Resultado:

- backend: verde em `test` e `verify`
- compose: valido em modo backend-only

## Regras

- teste verde sem documento de estado nao autoriza promover provider sozinho
- provider promovido precisa ter sua linha atualizada nas matrizes
- smoke live so atualiza `online_verified` se rodar com credenciais reais e output registrado

## Evidencia minima por mudanca

| Mudanca | Evidencia minima |
|---|---|
| novo provider textual | unit + integration + connectivity test |
| novo provider vetorial | unit + integration |
| grounded chat | unit + integration com citations/grounding |
| provider enterprise | docs oficiais + readiness honesto + integration |
| threat-intel | compliance gate + integration + evidencia de redaction |
