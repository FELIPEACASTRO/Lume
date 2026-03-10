# Register de perguntas, lacunas e premissas da Lume

Uso: obrigatorio durante Fase 0 e revisitado no inicio de cada fase posterior.

## Escala de classificacao

- `critica`: bloqueia arquitetura, escopo ou modelo comercial
- `alta`: nao bloqueia imediatamente, mas muda backlog, custo ou risco de forma relevante
- `media`: afeta detalhamento de implementacao
- `baixa`: afeta refinamento, nao direcao

## Registro atual

| ID | Tema | Pergunta em aberto | Impacto | Severidade | Premissa provisoria | Acao para fechar |
| --- | --- | --- | --- | --- | --- | --- |
| Q-001 | Benchmark | Qual a cobertura real acessivel do benchmark Manus alem das areas ja observadas? | Muda equivalencia funcional declarada | critica | Usar Manus observado + repo atual + inferencia controlada | Expandir varredura e registrar telas inacessiveis |
| Q-002 | Auth | Decisao travada: managed B2B auth no lancamento, com backend Spring como fronteira oficial de autorizacao e tenancy. | Fecha sessao, org, RBAC, rollout e prepara SSO futuro | critica | Managed B2B auth com claims mapeadas para `user -> membership -> role -> workspace` | Registrar ADR, definir provedor e implementar sem reabrir a tese |
| Q-003 | Billing | Decisao travada: Stripe-first global, mantendo billing central + BYOK e pass-through controlado por metering. | Fecha subscriptions, invoices, entitlements e accounting base | critica | Stripe como provedor inicial e metering proprio do Lume como fonte de reconciliacao | Registrar ADR, detalhar webhooks e separar Fase 2 vs Fase 4 |
| Q-004 | BYOK | Quais provedores permitirao BYOK no lancamento e com que limites? | Muda UX, seguranca e suporte | alta | BYOK para Tier 1 com segredo por workspace | Levantar requisitos oficiais por provedor |
| Q-005 | Compliance | Decisao travada: baseline minimo do MVP = LGPD/GDPR, com extensoes regionais apos validacao comercial. | Fecha retention, masking, audit e data governance minima | critica | Compliance baseline global B2B com LGPD/GDPR desde o core | Registrar matriz de dados sensiveis e backlog complementar por mercado |
| Q-006 | Search | Decisao travada: bootstrap de busca = `Postgres full-text + search_documents + rebuild jobs`. | Fecha indexacao inicial e evita stack prematura | alta | Busca keyword-first no core, com evolucao hibrida apenas sob pressao real | Registrar ADR e definir contrato de indexacao do workspace |
| Q-007 | Multimodal | Decisao travada: core obrigatorio do lancamento = `texto + embeddings + image`. | Fecha escopo inicial dos conectores e da fila | alta | Audio, video e realtime entram apenas depois do core operacional | Produzir matriz Tier 1/Tier 2 e refletir nos contratos do playground |
| Q-008 | Prompt assets | Outputs e prompts serao retidos integralmente ou por politica configuravel? | Muda persistencia, custo e compliance | alta | Retencao configuravel por tenant com minimo seguro | Definir politica por plano e compliance |
| Q-009 | Observabilidade | Qual stack base de tracing, metrics e logs sera usada? | Muda custo, operacao e engenharia | media | Observabilidade padronizada desde Fase 1 | Selecionar stack e registrar ADR |
| Q-010 | Files | Qual storage de arquivos e qual politica de malware scanning/retencao serao adotados? | Muda uploads, multimodal e custo | media | Object storage com politicas por tenant | Fechar stack de storage e seguranca |
| Q-011 | Enterprise | Decisao travada: `SSO/SCIM` entram depois do core operacional e nao bloqueiam o lancamento do MVP. | Fecha escopo enterprise inicial e protege o caminho critico | alta | Enterprise pesado apos auth, tenancy, inference e billing base estarem estaveis | Revisar apenas quando Fase 2 estiver operacionalizada |
| Q-012 | Benchmark competitivo | Quais AI gateways comparaveis vao pautar pricing e posicionamento? | Muda packaging e narrativa comercial | media | Comparar plataformas lideres na Fase 0 | Produzir benchmark competitivo oficial |
| Q-013 | Tier 1 | Quais provedores entram como Tier 1 oficial da Lume? | Muda seeds, contract tests, playground e BYOK | critica | Tier 1 oficial: `OpenAI + Anthropic + Google + xAI` | Validar catalogo com fontes oficiais e congelar seeds iniciais |

## Regras de uso

- Nenhuma pergunta critica pode sumir do roadmap sem decisao registrada.
- Toda premissa provisoria precisa de dono, data e criterio de revisao quando o register evoluir.
- Toda resposta externa precisa citar fonte ou stakeholder responsavel.
