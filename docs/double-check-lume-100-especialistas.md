# Double Check do Lume com 100 Especialistas

## Desdobramento oficial

Este diagnostico foi convertido em programa de execucao e arquitetura nos seguintes artefatos:

- `docs/roadmap-mestre-lume-ai-aggregation-platform.md`
- `docs/fase-0-pacote-mestre-lume.md`
- `docs/register-perguntas-lacunas-lume.md`

Data da auditoria: 2026-03-07  
Escopo: frontend real atual do repositorio `Lume`, com o shell React/Vite/Tailwind como fonte de verdade.  
Metodo: leitura de codigo, validacao de `pnpm lint`, validacao de `pnpm build`, smoke runtime em browser para `"/"`, `"/users"`, `"/agents"` e `"/library"`, mais leitura comparativa dos anexos conceituais.

## Nota metodologica

Este pacote usa `100 vozes especializadas` como estrutura de auditoria.  
Quando um nome proprio aparece no painel, isso representa uma `sintese metodologica do tipo de critica daquela escola`, nao uma citacao literal nem um endosso real do especialista.

## Fonte de verdade

- O frontend atual e `React 18 + Vite + React Router`, nao `Next.js`.
- As rotas publicas reais hoje sao `"/"`, `"/users"`, `"/agents"` e `"/library"`.
- O modulo `"/users"` e a principal area `live`.
- As areas `"/agents"` e `"/library"` continuam `preview`, sustentadas por fixtures locais.
- Os documentos anexos funcionam como `benchmark de ambicao` e `referencia teorica`, mas nao descrevem o estado exato do repositorio atual.

## Evidencia usada

- Leitura de codigo em:
  - `frontend/src/components/shell/AppShell.tsx`
  - `frontend/src/components/shell/Sidebar.tsx`
  - `frontend/src/components/shell/TopBar.tsx`
  - `frontend/src/components/shell/SearchModal.tsx`
  - `frontend/src/pages/Home.tsx`
  - `frontend/src/pages/Users.tsx`
  - `frontend/src/pages/Agents.tsx`
  - `frontend/src/pages/Library.tsx`
  - `frontend/src/data/shell.ts`
  - `frontend/src/hooks/useUsers.ts`
  - `frontend/src/components/users/UserForm.tsx`
  - `frontend/src/components/users/UserTable.tsx`
  - `frontend/src/services/api.ts`
  - `frontend/src/styles/index.css`
- Validacoes executadas:
  - `pnpm lint`: OK
  - `pnpm build`: OK
  - browser smoke: OK nas quatro rotas principais
- Achado operacional do smoke:
  - o backend estava offline no momento do browser smoke, portanto `"/users"` foi auditado de forma honesta no estado de erro recuperavel, nao no fluxo de sucesso live.

## Diagnostico executivo

O Lume atual ja nao parece um mock cru. Ele tem shell coerente, direcao visual forte, taxonomia melhor do que a maioria dos workspaces em estagio inicial e uma tentativa seria de separar `live` de `preview`. Isso e um ganho real.

O problema central nao e estetico. O problema central agora e `verdade operacional`. O frontend esta perto de parecer um produto mais completo do que realmente e. Em especial:

- a chrome global ainda usa sinais de `produto real` mesmo quando o usuario navega por areas `preview`
- alguns numeros e atalhos ainda podem ser lidos como dados operacionais quando na pratica sao derivados de fixture, placeholder ou estado inicial
- o contrato da busca global melhorou, mas continua local e nao representa memoria real do workspace
- a acessibilidade melhorou bastante no modal, mas o drawer mobile ainda nao fecha completamente a porta para foco e leitura indevida quando oculto
- o modulo `users` comunica honestidade melhor do que antes, mas ainda pode parecer inconsistente em loading e na diferenca entre `backend offline`, `proxy quebrado` e `erro 500`

Em resumo: o produto saiu da fase de `casca bonita` e entrou na fase de `ajuste fino entre promessa e entrega`. Isso e positivo, mas exige mais rigor agora do que exigia antes.

## Scorecard sintetico

| Eixo | Nota | Leitura curta |
| --- | --- | --- |
| Clareza visual do shell | 8.7/10 | Forte, coeso e acima da media para o estagio atual |
| Verdade entre live e preview | 7.0/10 | Melhorou bastante, mas ainda tem sinais conflitantes |
| Navegacao e arquitetura de informacao | 8.1/10 | Boa base, ainda pouco orientada por fluxo de trabalho real |
| Findability e busca | 7.2/10 | Boa casca e boa taxonomia, indexacao ainda local |
| Acessibilidade e teclado | 6.9/10 | Modal melhor resolvido do que o drawer; ainda ha lacunas reais |
| Mobile e ergonomia | 7.1/10 | Responsivo, mas nao totalmente mobile-first |
| Estados assincronos e erro | 7.5/10 | `Users` esta mais honesto, mas ainda ha arestas |
| Design system e consistencia | 7.8/10 | Tokens e padroes existem, governanca ainda inicial |
| Confianca e credibilidade | 7.0/10 | O produto inspira mais do que prova |
| Prontidao para escala | 6.8/10 | Boa base de shell, contratos ainda implicitos em excesso |

## Parecer executivo

- A direcao geral do frontend esta correta.
- O shell atual e digno de evolucao, nao de descarte.
- O maior risco nao e "ficar feio"; e "parecer pronto demais".
- O produto precisa ser mais explicito sobre o que e `live`, `preview`, `loading`, `error` e `empty`.
- O proximo salto de maturidade nao depende de reinventar layout. Depende de `contrato`, `semantica`, `acessibilidade`, `fonte de dados` e `governanca de estados`.

## Contratos publicos auditados

### Contratos hoje honestos

- `"/users"` se apresenta como modulo real conectado ao backend.
- `"/agents"` e `"/library"` exibem sinalizacao de `preview`.
- A busca global usa a taxonomia atual do shell e nao as rotas antigas dos anexos.
- O fluxo de edicao de usuario ja assume que senha vazia significa preservar a senha atual.

### Contratos ainda implicitos demais

- O topbar mostra `Produto real` mesmo em paginas `preview`.
- O estado do drawer mobile fechado ainda depende apenas de translacao visual, nao de exclusao clara da arvore interativa.
- O search modal trata resultado ativo com `aria-current` em `button`, o que nao e a semantica mais correta.
- Os cards numericos de `"/users"` mostram `0` durante loading, o que pode ser percebido como dado real.

### Contratos que precisam ser formalizados para escalar

- Um contrato unico de estado para telas e blocos: `live`, `preview`, `loading`, `empty`, `error`.
- Um contrato de busca real do workspace, substituindo indexacao puramente local.
- Um contrato de experiencia para falhas de API: `offline`, `timeout`, `server error`, `proxy/config error`.
- Um contrato semantico de componente para `modal`, `drawer`, `status badge`, `notice`, `metric card` e `confirmacao`.

## Achados criticos e relevantes

### Criticidade alta

- O indicador global `Produto real` no topo enfraquece a honestidade operacional quando o usuario entra em `"/agents"` e `"/library"`.
- O drawer mobile fechado continua estruturalmente presente sem `aria-hidden` ou `inert`, o que pode poluir navegacao assistiva.
- O search modal usa `aria-current` em `button`, o que mistura semantica de navegacao com semantica de lista interativa.

### Criticidade media

- `"/users"` mostra cards com numeros durante loading; isso precisa de estado neutro ou skeleton.
- O erro de `"/users"` ainda nao diferencia com precisao `backend offline` de `erro interno`.
- `window.confirm` no fluxo de exclusao quebra coerencia de shell e de teclado.
- `UserTable` ainda nao usa o mesmo padrao semantico de status do restante do shell.

### Criticidade baixa

- O import de fonte via `@import` do Google Fonts continua com custo de performance, resiliencia e privacidade.
- Ainda ha warnings de future flags do React Router no ambiente de desenvolvimento.

## Mapa de consenso

### Consenso forte

- O shell visual e bom o bastante para continuar sendo a base do produto.
- A maior prioridade e `verdade do produto`, nao troca de layout.
- A separacao entre `live` e `preview` precisa ser ainda mais explicita e sistemica.
- O modal de busca ja e uma boa direcao, mas precisa virar contrato de acessibilidade completo.
- O proximo salto de valor depende de dados reais e estados reais, nao de mais ornamentacao.

### Consenso moderado

- O home deve se comportar mais como `hub de trabalho` e menos como vitrine aspiracional.
- O modulo `users` deve ser tratado como area de referencia de maturidade para o resto do sistema.
- O design system deve evoluir de utilitarios coerentes para componentes semanticamente fortes.

### Consenso operacional

- Nenhuma area sustentada por fixture deve parecer `live`.
- Nenhum estado de erro deve se disfarcar de vazio.
- Nenhum overlay escondido deve permanecer navegavel por teclado ou tecnologia assistiva.

## Mapa de dissenso

### Minimalismo premium vs transparencia brutal

- Uma escola quer manter o topo elegante e enxuto.
- Outra escola quer rotulos mais duros e explicitos, inclusive repetindo `preview` em pontos hoje sutis.
- Decisao recomendada: privilegie transparencia enquanto a maturidade funcional ainda esta em construcao.

### Home como hub vs home como cockpit

- Uma escola defende pagina inicial mais limpa, com menos densidade.
- Outra quer maior densidade operacional logo na entrada.
- Decisao recomendada: manter home como hub, mas com caminho mais claro para as areas live.

### Search global ampla vs search tipada

- Uma escola favorece uma busca unica para reduzir custo cognitivo.
- Outra defende filtros por tipo e origem para aumentar confianca.
- Decisao recomendada: manter busca unica, mas explicitar tipo de resultado, estado e fonte de dados.

### Preview util vs preview perigoso

- Uma escola ve valor em previews para validar shell e narrativa.
- Outra ve risco de erosao de confianca quando preview parece funcional demais.
- Decisao recomendada: preview so pode existir com rotulacao dura, copy honesta e CTA sem promessas falsas.

## Novo roadmap consolidado

## P0

### 1. Corrigir a chrome global para refletir o estado real da rota

- Trilha: verdade do produto
- O que mudar: substituir o selo fixo `Produto real` por um indicador que mude conforme a rota e o bloco atual.
- Por que mudar: hoje o topo contradiz paginas `preview`.
- Impacto esperado: aumento imediato de confianca e reducao de ambiguidade.
- Dependencias: mapeamento central de `WorkspaceDataState`.
- Criterio de aceite: `"/"`, `"/users"`, `"/agents"` e `"/library"` exibem estado global coerente com a realidade da tela.

### 2. Fechar o drawer mobile para teclado e tecnologia assistiva quando oculto

- Trilha: acessibilidade e teclado
- O que mudar: aplicar semantica de ocultacao real no aside mobile fechado e revisar foco de abertura/fechamento.
- Por que mudar: translacao visual sem isolamento semantico nao basta.
- Impacto esperado: melhoria direta de acessibilidade e previsibilidade de navegacao.
- Dependencias: contrato unico para overlay, dialog e drawer.
- Criterio de aceite: com drawer fechado, elementos internos nao recebem foco nem aparecem como navegaveis em leitura assistiva.

### 3. Corrigir a semantica do SearchModal

- Trilha: acessibilidade e teclado
- O que mudar: trocar `aria-current` por padrao apropriado de lista ativa e revisar semantica de resultados.
- Por que mudar: o modal esta bom, mas ainda nao esta semanticamente limpo.
- Impacto esperado: experiencia mais robusta para teclado e leitores de tela.
- Dependencias: componente de lista navegavel padronizado.
- Criterio de aceite: foco, setas, Enter, Escape e leitura assistiva funcionam sem semantica incorreta.

### 4. Tornar metricas de `"/users"` honestas em loading e erro

- Trilha: maturidade funcional e confianca
- O que mudar: trocar `0` inicial por skeleton, placeholder neutro ou estado indisponivel.
- Por que mudar: `0` pode ser lido como dado real.
- Impacto esperado: reducao de falsa interpretacao operacional.
- Dependencias: componente reutilizavel de `metric card` com estados.
- Criterio de aceite: nenhum card numerico mostra valor real antes do carregamento terminar.

### 5. Diferenciar falha offline, falha de proxy e erro de servidor em `"/users"`

- Trilha: maturidade funcional e confianca
- O que mudar: enriquecer o mapeamento de erro no client e a copy exibida ao usuario.
- Por que mudar: "erro interno" nem sempre descreve a falha real.
- Impacto esperado: suporte mais rapido e diagnostico mais preciso.
- Dependencias: camada de erro padronizada em `api.ts`.
- Criterio de aceite: usuario recebe mensagem distinta para offline, timeout e erro 5xx.

## P1

### 6. Trocar `window.confirm` por confirmacao integrada ao shell

- Trilha: design system e consistencia
- O que mudar: criar dialog de confirmacao coerente com modal e teclado.
- Por que mudar: o confirm nativo quebra fluxo, estilo e acessibilidade.
- Impacto esperado: coerencia visual e comportamental maior.
- Dependencias: componente `ConfirmDialog`.
- Criterio de aceite: exclusao de usuario usa dialog do sistema e respeita teclado/foco.

### 7. Consolidar o contrato `live/preview/loading/empty/error`

- Trilha: verdade do produto
- O que mudar: formalizar um estado compartilhado para paginas, cards, tabelas e badges.
- Por que mudar: hoje a logica existe, mas ainda esta distribuida e parcialmente implicita.
- Impacto esperado: consistencia e menos regressao semantica.
- Dependencias: tipos centrais e componentes base.
- Criterio de aceite: todo bloco assinado por dado exibe seu estado de forma uniforme.

### 8. Reposicionar o home como hub explicitamente operacional

- Trilha: navegacao e findability
- O que mudar: reforcar caminhos para `users`, contextualizar melhor `agents` e `library` como preview e reduzir promessas implicitas do composer.
- Por que mudar: o home ainda comunica mais ambicao do que operacao real.
- Impacto esperado: descoberta melhor e menos expectativa errada.
- Dependencias: revisao de microcopy e CTA.
- Criterio de aceite: um usuario novo entende em 10 segundos o que funciona hoje e o que ainda e preview.

### 9. Padronizar `status`, `notice`, `metric card` e `empty state`

- Trilha: design system e consistencia
- O que mudar: promover esses padroes a componentes semanticamente oficiais.
- Por que mudar: a linguagem visual esta perto de ser sistema, mas ainda nao e governada o suficiente.
- Impacto esperado: manutencao mais simples e menos divergencia entre telas.
- Dependencias: inventario dos componentes comuns atuais.
- Criterio de aceite: as quatro rotas principais usam as mesmas primitivas de estado.

## P2

### 10. Evoluir a busca global para uma indexacao real do workspace

- Trilha: navegacao e findability
- O que mudar: substituir a indexacao local por fonte real de dados ou, no curto prazo, explicitar melhor a origem de cada resultado.
- Por que mudar: a busca atual funciona como shell demo inteligente.
- Impacto esperado: ganho real de produtividade e memoria operacional.
- Dependencias: contrato de busca e fontes indexaveis.
- Criterio de aceite: resultados possuem tipo, origem, estado e last update identificaveis.

### 11. Levar `agents` e `library` para contratos reais ou endurecer ainda mais o preview

- Trilha: maturidade funcional e confianca
- O que mudar: integrar backend ou reduzir ainda mais qualquer ambiguidade de completude.
- Por que mudar: preview prolongado demais desgasta confianca.
- Impacto esperado: produto mais coerente e menos "teaser permanente".
- Dependencias: priorizacao de backend e definicao de escopo.
- Criterio de aceite: cada pagina tem contrato live ou preview inequivoco e verificavel.

### 12. Melhorar a semantica mobile-first da topbar e da sidebar

- Trilha: mobile e responsividade
- O que mudar: revisar prioridade de acoes, densidade e leitura do topo em telas estreitas.
- Por que mudar: hoje e responsivo, mas ainda com DNA majoritariamente desktop.
- Impacto esperado: uso mais fluido em celular e tablet.
- Dependencias: testes em breakpoints reais e navegacao por polegar.
- Criterio de aceite: fluxos principais exigem menos scroll e menos ambiguidade no mobile.

### 13. Revisar carregamento de fontes e peso do CSS

- Trilha: mobile e responsividade
- O que mudar: reduzir dependencia de `@import` remoto e rever custo de renderizacao inicial.
- Por que mudar: performance e resiliencia importam ainda mais em shell persistente.
- Impacto esperado: carregamento mais estavel e previsivel.
- Dependencias: decisao sobre self-hosting ou fallback local.
- Criterio de aceite: fonte critica nao bloqueia experiencia inicial.

## P3

### 14. Introduzir visao orientada por papel ou objetivo

- Trilha: navegacao e findability
- O que mudar: diferenciar melhor fluxos para operador, gestor ou analista, sem fragmentar demais o shell.
- Por que mudar: a base atual serve bem a um usuario generico, mas ainda pouco a objetivos especificos.
- Impacto esperado: aumento de relevancia do home e da busca.
- Dependencias: pesquisa de uso real.
- Criterio de aceite: cada perfil central encontra seu primeiro passo com menos friccao.

### 15. Instrumentar o produto para medir confianca e descoberta

- Trilha: maturidade funcional e confianca
- O que mudar: medir clique em preview, falha em busca, retry de erro, abandono de formulario e navegacao mobile.
- Por que mudar: o proximo ciclo precisa sair de opiniao e entrar em evidencia.
- Impacto esperado: backlog mais preciso e menos suposicao.
- Dependencias: definicao de eventos e politica de analytics.
- Criterio de aceite: existe painel minimo para entender uso, erro e descoberta das rotas atuais.

## Sequencia recomendada de execucao

1. P0 inteiro
2. P1 itens 6, 7 e 8
3. P1 item 9
4. P2 itens 10 e 11
5. P2 item 12
6. P2 item 13
7. P3 inteiro

## Veredito final

O Lume atual merece continuar sendo evoluido sobre a base que ja existe. O shell esta suficientemente bom para sustentar produto real. O que falta nao e uma nova camada de polish; falta reduzir a distancia entre `o que a interface sugere` e `o que o sistema realmente entrega hoje`.

Se o time executar bem o P0 e o P1, o produto deixa de parecer "quase pronto" e passa a parecer `confiavel`. Esse e o salto correto neste momento.
