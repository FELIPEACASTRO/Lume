# Fase 0 do Lume: pacote mestre de descoberta e especificacao

## Objetivo

Fechar a engenharia reversa do benchmark e produzir o pacote completo de especificacao exigido para que a Lume possa ser implementada como AI Aggregation Platform sem lacunas estruturais.

## Entradas obrigatorias

- `docs/double-check-lume-100-especialistas.md`
- `docs/painel-100-especialistas-lume.md`
- benchmark Manus ja inspecionado
- repositorio atual do Lume
- pesquisa web em fontes oficiais sobre provedores, modelos, billing, autenticacao, limites e seguranca

## Regras de evidencia

Cada item documentado deve ser marcado como:

- `observado diretamente`
- `inferido com alta confianca`
- `benchmark externo`
- `decisao arquitetural proposta`

Nenhum texto deve misturar essas categorias sem rotular a origem da afirmacao.

## Entregaveis da Fase 0

### Bloco 1. Benchmark e navegacao

- resumo executivo da solucao analisada
- mapa completo da navegacao
- taxonomia de modulos, telas, componentes e fluxos
- inventario do frontend observado

### Bloco 2. Funcionalidade e negocio

- matriz exaustiva de funcionalidades
- especificacao funcional detalhada
- especificacao de negocio da Lume
- estrategia de produto, papeis, permissao, planos e governanca

### Bloco 3. Tecnologia e dados

- especificacao tecnica completa
- contratos de API
- modelo de dados e banco
- matriz de rastreabilidade frontend -> backend -> banco -> observabilidade -> testes

### Bloco 4. Agregador de IA

- catalogo global de solucoes de IA com API
- estrategia de priorizacao Tier 1, Tier 2 e Tier 3
- arquitetura especifica da AI Aggregation Layer
- estrategia de onboarding continuo de conectores

### Bloco 5. Marca, UX e frontend proposto

- design system original da Lume
- frontend proposto da Lume
- guidelines de responsividade, copy, loading, erro e vazio

## Sequencia de execucao

1. Consolidar o inventario do benchmark acessivel
2. Registrar lacunas, itens nao observaveis e impacto
3. Pesquisar provedores e concorrentes em fontes oficiais
4. Fechar taxonomia de dominios do produto
5. Derivar especificacao funcional
6. Derivar especificacao de negocio
7. Derivar especificacao tecnica
8. Derivar APIs, eventos e modelo de dados
9. Fechar matriz de rastreabilidade
10. Fechar design system e frontend proposto

## Gating de qualidade

### GQ1. Benchmark

- toda tela acessivel deve estar inventariada
- toda inferencia deve estar marcada como inferencia

### GQ2. Funcionalidade

- nenhuma funcionalidade de frontend fica sem regra de negocio
- nenhuma funcionalidade fica sem backend e dados

### GQ3. Integracoes

- cada provedor listado precisa de fonte oficial e data de verificacao
- cada integracao priorizada precisa de risco, esforco e valor documentados

### GQ4. Tecnologia

- toda API precisa de auth, autorizacao, schema, erros e eventos
- toda entidade precisa de persistencia, indice, retencao e auditoria quando aplicavel

### GQ5. UX

- cada tela precisa de estados `loading`, `empty`, `error`, `partial`, `success`, `conflict`, `retry`, `limit reached`, `permission denied`, `onboarding` e responsividade quando aplicavel

## Estrutura minima de saida esperada

- `secao-01-resumo-executivo`
- `secao-02-mapa-da-navegacao`
- `secao-03-inventario-do-frontend`
- `secao-04-matriz-de-funcionalidades`
- `secao-05-especificacao-funcional`
- `secao-06-especificacao-de-negocio`
- `secao-06a-catalogo-global-de-apis-de-ia`
- `secao-06b-priorizacao-de-integracoes`
- `secao-07-especificacao-tecnica`
- `secao-07a-arquitetura-especifica-do-agregador`
- `secao-08-contratos-de-api`
- `secao-09-modelo-de-dados`
- `secao-10-matriz-de-rastreabilidade`
- `secao-11-design-system-lume`
- `secao-12-frontend-proposto`
- `secao-13-stack-sugerida`
- `secao-14-plano-de-implementacao`
- `secao-15-lacunas-inferencias-riscos`

## Dependencias criticas

- benchmark acessivel o suficiente para inventario honesto
- definicao de fontes oficiais aceitaveis por provedor
- alinhamento de auth B2B e billing hibrido
- decisao sobre segredos, storage e observabilidade base

## Riscos da Fase 0

- benchmark parcial induzir falsa equivalencia funcional
- catalogo de provedores mudar durante a pesquisa
- ausencia de resposta de negocio em politicas de plano, regiao e compliance
- inflacao documental sem hierarquia de importancia

## Mitigacoes

- manter register de perguntas e lacunas vivo
- usar data de verificacao em toda fonte externa
- separar claramente `observado` de `proposto`
- priorizar profundidade nos modulos core antes de expandir o long tail

## Criterio de pronto

A Fase 0 so fecha quando:

- o benchmark acessivel estiver integralmente inventariado
- a Lume estiver descrita como produto, negocio, arquitetura e dados
- cada funcionalidade do frontend possuir backend, persistencia, observabilidade e testes propostos
- o catalogo inicial de integracoes estiver tierizado e fundamentado
- o time conseguir iniciar implementacao sem depender de decisoes estruturais em aberto
