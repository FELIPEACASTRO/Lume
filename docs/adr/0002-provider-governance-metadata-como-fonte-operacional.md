# ADR 0002 - Provider governance metadata como fonte operacional

## Decisao

`provider-governance-metadata.json` vira overlay oficial de estado, evidencia e metadata operacional por provider.

## Motivo

- evitar hardcode disperso de status, prioridade e posture comercial
- reconciliar docs, roadmap e runtime com uma fonte tecnicamente consumível

## Consequencia

- qualquer mudança de status de provider exige ajuste no overlay e nas matrizes
- docs humanas e API publica devem refletir o mesmo estado

