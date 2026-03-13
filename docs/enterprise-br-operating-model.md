# Lume Operating Model: Enterprise Brasil

## Fonte de verdade
- `codigo + testes verdes` prevalecem sobre qualquer documento.
- `provider-governance-metadata.json` e contratos HTTP versionados definem readiness e exposicao operacional.
- docs do repositório explicam a solucao; anexos externos entram como referencia de estrategia, nao como verdade executavel.

## Meta de produto
- Segmento principal: `Enterprise Brasil`.
- KPI primario de 180 dias: `NRR + margem de contribuicao`.
- Wedge: `FinOps + governanca`.
- GTM: `hibrido (direto + canal/parceiros)`.

## Caminho critico
1. `Ativacao core`: onboarding, primeiro projeto, primeira tarefa, primeira conversa real, primeiro template.
2. `Operacao diaria`: projetos, tarefas, biblioteca, equipe e configuracoes com estado real.
3. `Billing confiavel`: checkout, webhook assinado, idempotencia, reconciliacao e dunning.
4. `FinOps`: ledgers, budgets, anomalias, kill switch, antiabuso e scorecard.
5. `Team + BYOK`: somente depois de billing, admin e auditoria estaveis.

## Itens fora do caminho critico
- marketplace aberto
- white-label amplo
- voz realtime pesada
- video generativo pesado
- automacoes autonomas complexas
- replatform de stack
- microservices sem dor operacional comprovada

## Regra de release
- nada entra como `live` sem `API + persistencia + observabilidade + auditoria + teste`.
