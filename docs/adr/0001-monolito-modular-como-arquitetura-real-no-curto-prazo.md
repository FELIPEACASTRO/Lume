# ADR 0001 - Monolito modular como arquitetura real no curto prazo

## Decisao

O Lume continua monolito modular no curto prazo.

## Motivo

- o runtime capability-first ainda esta em consolidacao
- o custo de extracao para reactor multi-modulo ainda nao foi justificado por build pain ou ownership boundary fortes

## Consequencia

- aprofundar governanca, contratos e testes agora
- reavaliar reactor multi-modulo somente com dados de release/performance

