# Painel de 100 Especialistas do Lume

Escopo: frontend real atual do Lume.  
Base de julgamento: shell React/Vite de hoje, nao a solucao descrita em documentos antigos.  
Formato: cada voz emite quatro pontos curtos.

## Conselho 1: UX fundacional e design centrado no humano

### 1. Don Norman
- Aprova: a interface passou a sinalizar melhor `live` e `preview`.
- Reprova: a chrome global ainda cria `signifiers` conflitantes em paginas preview.
- Risco principal: o usuario acreditar em uma capacidade que a tela ainda nao entrega.
- Recomendacao objetiva: propagar o estado real da rota para topo, cards, CTAs e busca.

### 2. Jakob Nielsen
- Aprova: a consistencia geral do shell e forte.
- Reprova: a visibilidade do estado do sistema ainda nao diferencia bem offline, erro 500 e preview.
- Risco principal: perda de confianca por falta de feedback preciso.
- Recomendacao objetiva: padronizar mensagens e indicadores de estado em toda a shell.

### 3. Jesse James Garrett
- Aprova: superficie, esqueleto e estrutura visual estao mais alinhados.
- Reprova: a estrategia funcional ainda e menor do que a arquitetura de informacao sugere.
- Risco principal: a camada de superficie vender mais do que o escopo real.
- Recomendacao objetiva: alinhar promessa do home e da busca ao escopo operacional valido hoje.

### 4. Luke Wroblewski
- Aprova: o shell responde bem em telas menores.
- Reprova: a experiencia ainda tem DNA prioritariamente desktop.
- Risco principal: mobile virar apenas adaptacao visual, nao fluxo de trabalho real.
- Recomendacao objetiva: redesenhar os fluxos principais partindo de `390px` antes de expandir.

### 5. Brad Frost
- Aprova: existe uma base visivel de tokens, badges, notices e superficies.
- Reprova: os contratos de componente ainda nao estao formalizados o bastante.
- Risco principal: drift visual e semantico entre telas conforme o produto crescer.
- Recomendacao objetiva: transformar estados e padroes em primitives oficiais do design system.

### 6. Steve Krug
- Aprova: a navegacao principal esta mais facil de entender.
- Reprova: o usuario ainda precisa pensar demais sobre o que e real e o que e demonstrativo.
- Risco principal: hesitacao na primeira exploracao do produto.
- Recomendacao objetiva: simplificar microcopy e deixar o preview impossivel de confundir com live.

### 7. Indi Young
- Aprova: o produto tem tom operacional e menos exibicionista.
- Reprova: ainda nao comunica bem modelos mentais diferentes de uso.
- Risco principal: uma mesma shell servir medianamente para todos e perfeitamente para ninguem.
- Recomendacao objetiva: mapear top tasks por tipo de usuario e refletir isso no home.

### 8. Alan Cooper
- Aprova: a base atual pode suportar design orientado por objetivos.
- Reprova: o fluxo principal ainda parece mais orientado por modulo do que por objetivo.
- Risco principal: backlog crescer por feature e nao por meta do usuario.
- Recomendacao objetiva: explicitar a persona primaria e fazer o home prioriza-la.

### 9. Susan Weinschenk
- Aprova: a hierarquia visual reduz carga cognitiva inicial.
- Reprova: numeros e badges ainda podem ser lidos como provas operacionais fortes demais.
- Risco principal: erosao de credibilidade quando o usuario percebe o descompasso.
- Recomendacao objetiva: tornar todos os sinais cognitivos coerentes com a realidade do dado.

### 10. Tim Brown
- Aprova: o frontend ja e um prototipo suficientemente evoluido para validacao real.
- Reprova: ainda falta um contrato claro entre backstage tecnico e frontstage de estados.
- Risco principal: polish crescer mais rapido do que confiabilidade.
- Recomendacao objetiva: desenhar mapa de estados e fontes de dados antes de ampliar o shell.

## Conselho 2: usabilidade heuristica e learnability

### 11. Bruce Tognazzini
- Aprova: o produto reduz ruido visual e concentra acoes principais.
- Reprova: overlays ocultos ainda nao estao semanticamente fechados no mobile.
- Risco principal: comportamento inesperado em foco e navegacao assistiva.
- Recomendacao objetiva: tratar drawer e modal como componentes com regras de interacao estritas.

### 12. Whitney Quesenbery
- Aprova: a experiencia esta mais eficiente do que antes.
- Reprova: ainda nao e totalmente `easy` e `error tolerant` em estados de falha.
- Risco principal: um usuario novo nao entender se falhou a tela, a API ou o dado.
- Recomendacao objetiva: explicitar causa, proximo passo e impacto em toda mensagem de erro.

### 13. Ginny Redish
- Aprova: a copy geral esta curta e operacional.
- Reprova: alguns textos ainda falam mais da interface do que da tarefa do usuario.
- Risco principal: linguagem bonita mas pouco orientada a acao.
- Recomendacao objetiva: reescrever titulos e descricoes segundo verbo, objeto e consequencia.

### 14. Ben Shneiderman
- Aprova: ha tentativa clara de fechamento com Enter, Escape e feedback visual.
- Reprova: a semantica da lista ativa na busca ainda nao esta resolvida com rigor.
- Risco principal: usuarios de teclado encontrarem comportamento coerente visualmente, mas fraco semanticamente.
- Recomendacao objetiva: padronizar o modelo de lista navegavel com foco e item ativo corretos.

### 15. Larry Tesler
- Aprova: parte da complexidade do shell foi escondida do usuario final.
- Reprova: a distincao entre dado real e dado de fixture ainda escapa do sistema para o usuario.
- Risco principal: complexidade mal escondida reaparecer como desconfianca.
- Recomendacao objetiva: empurrar a complexidade de estado para componentes e contratos internos.

### 16. Rolf Molich
- Aprova: o produto esta pronto para testes moderados de usabilidade.
- Reprova: ainda depende demais da interpretacao do time sobre o que ficou claro.
- Risco principal: assumir learnability sem evidencias observacionais.
- Recomendacao objetiva: executar testes curtos com tarefas reais nas quatro rotas principais.

### 17. Caroline Jarrett
- Aprova: o formulario de usuario esta mais limpo e focal.
- Reprova: a experiencia de confirmacao e exclusao ainda usa `window.confirm`.
- Risco principal: quebra de contexto, estilo e acessibilidade no fluxo de form.
- Recomendacao objetiva: criar confirmacao integrada ao shell com foco, copy e acoes consistentes.

### 18. Jared Spool
- Aprova: a interface oferece melhor `information scent` do que antes.
- Reprova: o caminho do home para valor real ainda disputa espaco com areas preview.
- Risco principal: usuario seguir o cheiro mais forte para uma area menos util.
- Recomendacao objetiva: reforcar no home a trilha para `users` e para busca real do workspace.

### 19. Gerry McGovern
- Aprova: ha um nucleo operacional identificavel.
- Reprova: o produto ainda nao deixa claro quais sao as `top tasks` de hoje.
- Risco principal: a homepage virar portfolio de capacidades em vez de pagina de trabalho.
- Recomendacao objetiva: escolher de uma a tres top tasks e privilegiar tudo que as acelera.

### 20. Janice Fraser
- Aprova: a experiencia tem direcao e ja sustenta discussao de produto.
- Reprova: a shell ainda pede mais maturidade de comportamento do que de aparencia.
- Risco principal: confundir refinamento visual com refinamento de uso.
- Recomendacao objetiva: tratar o proximo ciclo como ciclo de uso e nao de estetica.

## Conselho 3: arquitetura de informacao e navegacao

### 21. Peter Morville
- Aprova: a taxonomia principal do shell e mais limpa e mais achavel.
- Reprova: a busca ainda indexa contexto local e nao um espaco de informacao real.
- Risco principal: boa findability aparente sem profundidade real de recuperacao.
- Recomendacao objetiva: evoluir a busca para origem, tipo, estado e atualizacao do resultado.

### 22. Louis Rosenfeld
- Aprova: a navegacao lateral ja possui boa separacao por grupos.
- Reprova: `Workspace`, `Areas reais` e `Areas preview` ainda podem parecer categorias do mesmo peso operacional.
- Risco principal: usuario achar que preview e apenas outra area ja pronta.
- Recomendacao objetiva: diferenciar agrupamento informacional de agrupamento operacional.

### 23. Abby Covert
- Aprova: a estrutura atual e suficientemente nomeavel.
- Reprova: ainda ha ambiguidades em conceitos como `workspace`, `hub`, `agents` e `biblioteca`.
- Risco principal: cada pessoa atribuir significado diferente aos mesmos rotulos.
- Recomendacao objetiva: fechar um glossario de produto e refletir esse glossario na interface.

### 24. Jorge Arango
- Aprova: o shell sugere ecologia informacional coerente.
- Reprova: o sistema ainda nao evidencia com clareza a relacao entre navegar, buscar e operar.
- Risco principal: comportamento de orientacao fragmentado.
- Recomendacao objetiva: declarar com mais clareza como o workspace se organiza e onde cada coisa vive.

### 25. Donna Spencer
- Aprova: a navegacao tem grupos reconheciveis.
- Reprova: ainda falta uma regra forte para quando algo merece virar item lateral.
- Risco principal: crescimento da sidebar por acumulacao de modulos.
- Recomendacao objetiva: documentar criterio de entrada na navegacao principal.

### 26. Dan Brown
- Aprova: existe um esqueleto navegacional com cara de produto escalavel.
- Reprova: o sistema ainda nao mostra seus principios de arquitetura informacional.
- Risco principal: cada nova tela chegar com padrao proprio.
- Recomendacao objetiva: registrar principios de IA e usar esses principios como gate de novas rotas.

### 27. Karen McGrane
- Aprova: a interface comeca a respeitar contexto de uso real.
- Reprova: ainda ha microcopy e densidade de informacao pouco calibradas para diferentes telas.
- Risco principal: o mesmo conteudo funcionar bem no desktop e mal no mobile.
- Recomendacao objetiva: fazer inventario de conteudo por breakpoint e por prioridade.

### 28. Kristina Halvorson
- Aprova: o texto nao esta mais servindo apenas a ornamentacao.
- Reprova: a governanca do conteudo ainda parece informal.
- Risco principal: labels, descricoes e notices divergirem com o tempo.
- Recomendacao objetiva: tratar copy como parte do sistema e nao como ultimo ajuste.

### 29. Rachel Lovinger
- Aprova: as rotas principais ja comunicam parte do modelo conceitual do produto.
- Reprova: `preview` e `live` ainda nao estao embutidos no proprio sistema de rotulos.
- Risco principal: o usuario carregar expectativas erradas ao ler o nome da area.
- Recomendacao objetiva: adicionar metadados de estado de forma mais estrutural aos rotulos e resultados.

### 30. Leisa Reichelt
- Aprova: existe uma boa base para service design digital.
- Reprova: a costura entre backstage indisponivel e frontstage elegante ainda nao esta totalmente franca.
- Risco principal: a jornada prometer continuidade onde ha interrupcao por falta de backend.
- Recomendacao objetiva: mapear pontos de ruptura de servico e desenhar respostas explicitas para cada um.

## Conselho 4: enterprise SaaS e produtividade operacional

### 31. Kim Goodwin
- Aprova: a shell atual ja permite discutir eficiencia de trabalho, nao so aparencia.
- Reprova: a organizacao ainda esta pouco orientada por contexto de uso e papel.
- Risco principal: produtividade media para todos, excelente para ninguem.
- Recomendacao objetiva: mapear cenarios centrais e ajustar home, busca e atalhos a esses cenarios.

### 32. Christina Wodtke
- Aprova: o produto mostra direcao e narrativa de workspace.
- Reprova: a experiencia ainda precisa de maior foco na meta do usuario ao entrar.
- Risco principal: usuario chegar, admirar a interface e ainda perguntar "por onde eu gero valor agora?".
- Recomendacao objetiva: transformar a pagina inicial em plano de voo operacional.

### 33. Marty Cagan
- Aprova: existe uma superficie forte para testar valor.
- Reprova: o risco de viabilidade e prontidao tecnica ainda nao esta totalmente exposto na interface.
- Risco principal: vender como produto consolidado o que ainda e modulo parcialmente demonstrativo.
- Recomendacao objetiva: alinhar roadmap visual e roadmap funcional em uma mesma linguagem.

### 34. Teresa Torres
- Aprova: o produto ja sustenta loops de descoberta baseados em tarefas.
- Reprova: ainda faltam mecanismos de medicao sobre busca, preview e retry.
- Risco principal: evoluir por opiniao interna em vez de evidencia de uso.
- Recomendacao objetiva: instrumentar eventos minimos antes do proximo salto de escopo.

### 35. Melissa Perri
- Aprova: a direcao do produto esta mais proxima de outcome do que antes.
- Reprova: os sinais de progresso ainda sao muito de interface e pouco de valor operacional.
- Risco principal: backlog virar manutencao de casca.
- Recomendacao objetiva: associar cada melhoria de shell a um resultado observavel no uso real.

### 36. Jeff Patton
- Aprova: os modulos ja podem ser lidos como partes de uma jornada.
- Reprova: essa jornada ainda nao esta explicitamente costurada.
- Risco principal: o usuario pular de pagina em pagina sem sentir continuidade de tarefa.
- Recomendacao objetiva: desenhar story map do fluxo atual com foco em `buscar`, `avaliar`, `operar`.

### 37. Jeff Gothelf
- Aprova: a shell esta pronta para experimentos enxutos.
- Reprova: ainda nao ha prova suficiente de que os refinamentos recentes atacaram as dores certas.
- Risco principal: iteracao rapida sem aprendizado claro.
- Recomendacao objetiva: definir hipoteses e metricas curtas para cada item do roadmap.

### 38. John Cutler
- Aprova: ha um sistema nascente de linguagem operacional.
- Reprova: ainda faltam loops de feedback fechados entre comportamento, erro e melhoria.
- Risco principal: a equipe ver o problema, mas nao medir recorrencia nem impacto.
- Recomendacao objetiva: ligar observabilidade de uso a priorizacao de UX.

### 39. Leah Buley
- Aprova: a experiencia esta mais empatica com o estado de erro do que em versoes anteriores.
- Reprova: a relacao entre expectativa e realidade ainda pede mais franqueza.
- Risco principal: expectativas inchadas reduzirem satisfacao mesmo quando a interface esta bonita.
- Recomendacao objetiva: tratar confianca como requisito de UX e nao como atributo colateral.

### 40. Des Traynor
- Aprova: o produto ja tem uma shell que parece software de trabalho.
- Reprova: ainda nao esta claro quais capacidades sustentam a proposta central do Lume a longo prazo.
- Risco principal: a interface parecer mais larga do que a estrategia.
- Recomendacao objetiva: defender um nucleo de produto e fazer o resto orbitar esse nucleo.

## Conselho 5: mobile, responsividade e ergonomia

### 41. Steven Hoober
- Aprova: o drawer mobile resolve descoberta basica de navegacao.
- Reprova: a ergonomia de alcance e de foco ainda pode melhorar.
- Risco principal: interacoes repetidas exigirem mais ajuste de mao e mais esforco do que o necessario.
- Recomendacao objetiva: testar alcances de polegar e prioridade de acoes na topbar mobile.

### 42. Josh Clark
- Aprova: o produto nao trata mobile como miniatura do desktop.
- Reprova: ainda falta aproveitar melhor o contexto e o ritmo de uso movel.
- Risco principal: parecer responsivo sem ser verdadeiramente util em movimento.
- Recomendacao objetiva: redesenhar a experiencia mobile a partir das acoes mais frequentes.

### 43. Ethan Marcotte
- Aprova: a estrutura liquida e os breakpoints suportam boa adaptacao.
- Reprova: responsividade ainda esta mais no layout do que na prioridade de conteudo.
- Risco principal: tudo caber, mas nem tudo merecer caber com o mesmo peso.
- Recomendacao objetiva: definir prioridade de conteudo por breakpoint e nao apenas ajustes de grid.

### 44. Jen Simmons
- Aprova: a composicao geral continua elegante mesmo quando comprimida.
- Reprova: alguns blocos ainda carregam a hierarquia do desktop para o mobile sem reorquestracao plena.
- Risco principal: boa forma visual com sequencia de leitura subotima.
- Recomendacao objetiva: rever ordem, espacamento e densidade em telas estreitas.

### 45. Theresa Neil
- Aprova: o sistema evita excesso de controles no mobile.
- Reprova: estados e pistas contextuais ainda podem competir com a area realmente acionavel.
- Risco principal: usuario tocar mais para descobrir do que para operar.
- Recomendacao objetiva: simplificar o topo mobile e reforcar sinais de acao primaria.

### 46. Rachel Hinman
- Aprova: a interface admite interacao tocavel sem colapsar.
- Reprova: ainda nao comunica suficientemente bem continuidade entre busca, navegacao e retorno.
- Risco principal: sensacao de deslocamento em fluxos curtos de ida e volta.
- Recomendacao objetiva: tornar o ciclo abrir, navegar e voltar mais previsivel no mobile.

### 47. Cyd Harrell
- Aprova: a shell ja pode suportar uso em contexto de trabalho real.
- Reprova: ainda falta pensar o uso com interrupcao, pressa e ambiente imperfeito.
- Risco principal: interface aparentemente robusta, mas fraca em condicoes reais de operacao.
- Recomendacao objetiva: testar tarefas moveis sob interrupcao e conexao instavel.

### 48. Nick Babich
- Aprova: a linguagem visual tem legibilidade boa e aspecto moderno.
- Reprova: a consistencia de feedback em hover, foco e toque ainda pode convergir mais.
- Risco principal: desktop e mobile parecerem parentes, nao o mesmo sistema.
- Recomendacao objetiva: equalizar estado interativo por input modality.

### 49. Faruk Ates
- Aprova: o shell preserva identidade visual em multiplos tamanhos.
- Reprova: ainda existe dependencia de alguns comportamentos e densidades herdados do desktop.
- Risco principal: adaptacao cosmetica em vez de responsiva de verdade.
- Recomendacao objetiva: revisar prioridades do layout em vez de apenas dimensoes.

### 50. Scott Jehl
- Aprova: a experiencia movel lanca uma base razoavel de resiliencia.
- Reprova: fonte remota e custo visual ainda podem afetar carregamento inicial.
- Risco principal: queda de qualidade percebida em redes piores.
- Recomendacao objetiva: reduzir dependencia de recursos remotos e otimizar o caminho critico.

## Conselho 6: acessibilidade, WCAG e navegacao por teclado

### 51. Sarah Horton
- Aprova: o produto assume foco visivel e sinalizacao mais forte do que muitas interfaces visuais equivalentes.
- Reprova: o drawer mobile fechado ainda nao esta fora da experiencia de navegacao como deveria.
- Risco principal: acessibilidade parcial mascarada por boa aparencia.
- Recomendacao objetiva: formalizar padroes de ocultacao, foco inicial e retorno de foco.

### 52. Heydon Pickering
- Aprova: a estrutura do modal de busca ja aponta para um dialog de verdade.
- Reprova: ainda ha semantica inventada onde deveria haver padrao nativo mais preciso.
- Risco principal: divergencia entre o que a interface aparenta e o que a arvore acessivel comunica.
- Recomendacao objetiva: revisar papeis ARIA com a pergunta "precisa mesmo existir?".

### 53. Leonie Watson
- Aprova: ha intencao clara de respeitar teclado e leitores de tela.
- Reprova: o modelo de item ativo da busca ainda nao comunica estado de selecao com a semantica ideal.
- Risco principal: experiencia assistiva funcional, mas menos compreensivel do que a visual.
- Recomendacao objetiva: adotar padrao semantico de lista navegavel e item ativo consistente.

### 54. Marcy Sutton
- Aprova: o produto deixou de ignorar completamente acessibilidade em overlays.
- Reprova: ainda falta um contrato testavel de acessibilidade para modal, drawer e notices.
- Risco principal: regressao silenciosa a cada ajuste de shell.
- Recomendacao objetiva: criar checklist e smoke tests de acessibilidade nas rotas principais.

### 55. Steve Faulkner
- Aprova: ha tentativa de estruturar melhor papeis, labels e foco.
- Reprova: elementos escondidos ainda podem permanecer ativos demais para tecnologia assistiva.
- Risco principal: navegacao "fantasma" em componentes fora de cena.
- Recomendacao objetiva: garantir que ocultacao visual implique ocultacao interativa.

### 56. Henny Swan
- Aprova: o produto comeca a considerar experiencia inclusiva como parte da qualidade.
- Reprova: ainda faltam validacoes mais sistemicas de contraste, target size e sequencia de foco.
- Risco principal: boa acessibilidade pontual, baixa acessibilidade sistemica.
- Recomendacao objetiva: transformar requisitos WCAG em criterios de aceite de PR.

### 57. Sarah Higley
- Aprova: o dialog de busca tem fundamento promissor.
- Reprova: o desenho dos estados de erro ainda pode ficar mais util para usuarios com diferentes necessidades cognitivas.
- Risco principal: informacao essencial ficar escondida em copy elegante demais.
- Recomendacao objetiva: priorizar clareza, acao e contexto em toda mensagem critica.

### 58. Adrian Roselli
- Aprova: a interface ja esta mais perto de um sistema navegavel do que de uma landing page.
- Reprova: ainda existem detalhes de semantica e foco que precisam de rigor tecnico maior.
- Risco principal: passar em inspecao visual e falhar em uso assistivo real.
- Recomendacao objetiva: validar com teclado puro e leitor de tela antes de expandir escopo.

### 59. Melanie Sumner
- Aprova: a equipe parece aberta a tratar acessibilidade como parte da engenharia.
- Reprova: ainda nao ha sinais de automatizacao dessa disciplina.
- Risco principal: melhorias manuais nao se sustentarem no tempo.
- Recomendacao objetiva: incluir auditoria automatica minima e smoke acessivel no fluxo de validacao.

### 60. Derek Featherstone
- Aprova: o produto ja tem base para evoluir sem reescrever tudo.
- Reprova: falta ainda uma definicao mais firme de severidade para problemas assistivos.
- Risco principal: acessibilidade ser tratada como ajuste fino, nao como requisito.
- Recomendacao objetiva: classificar achados de acessibilidade por impacto e bloquear regressao critica.

## Conselho 7: design systems, consistencia visual e escalabilidade

### 61. Nathan Curtis
- Aprova: o Lume ja exibe varios sinais de sistema, nao apenas de paginas independentes.
- Reprova: o nivel de governanca ainda esta abaixo do que a shell agora exige.
- Risco principal: componentes parecidos se multiplicarem com pequenas variacoes.
- Recomendacao objetiva: definir propriedade, documentacao e criterio de evolucao para cada primitive central.

### 62. Alla Kholmatova
- Aprova: ha linguagem visual consistente entre badge, panel, surface e shell.
- Reprova: a logica de estados ainda nao esta tao tokenizada quanto a estetica.
- Risco principal: consistencia visual sobreviver enquanto consistencia comportamental se perde.
- Recomendacao objetiva: vincular tokens visuais a estados de uso e nao apenas a estilo.

### 63. Jina Anne
- Aprova: o sistema ja tem materia-prima para documentacao clara.
- Reprova: ainda nao esta claro como o time decide o que entra no sistema e o que fica local.
- Risco principal: biblioteca informal crescer sem fronteiras.
- Recomendacao objetiva: escrever criterio de entrada e nivel de maturidade de componente.

### 64. Dan Mall
- Aprova: a identidade do shell e suficientemente forte para suportar padronizacao.
- Reprova: a composicao ainda depende muito da disciplina manual de cada tela.
- Risco principal: harmonia visual cair com novas entregas.
- Recomendacao objetiva: consolidar layouts e estados em blocos reutilizaveis de alto nivel.

### 65. Diana Mounter
- Aprova: os elementos centrais ja parecem pertencer a uma familia.
- Reprova: a consistencia de nomenclatura e de documentacao ainda nao aparece no repositorio.
- Risco principal: onboarding de novos contribuidores ficar caro.
- Recomendacao objetiva: dar nome oficial aos padroes e publicar seu uso esperado.

### 66. Amy Thibodeau
- Aprova: os componentes centrais possuem tom e peso visual coerentes.
- Reprova: a relacao entre conteudo, estado e estilo ainda pode ser mais sistemica.
- Risco principal: interfaces parecidas carregarem mensagens diferentes de modo desigual.
- Recomendacao objetiva: amarrar copy, estado e tratamento visual no mesmo contrato de componente.

### 67. Chris Coyier
- Aprova: o frontend tem boa legibilidade de CSS utilitario e identidade clara.
- Reprova: ainda falta um degrau entre utilitarios e componentes canonicos.
- Risco principal: repeticao sutil de classes virar manutencao cara.
- Recomendacao objetiva: promover padroes recorrentes a abstracoes nomeadas e rastreaveis.

### 68. Lente de governanca de tokens
- Aprova: existe base de cor, superficie, linha e foco.
- Reprova: ainda nao ha mapa explicito de onde cada token e obrigatorio, opcional ou proibido.
- Risco principal: drift visual invisivel aos poucos.
- Recomendacao objetiva: documentar tokens por intencao, nao apenas por valor.

### 69. Lente de documentacao de componentes
- Aprova: o sistema ja possui primitives candidatas a documentacao viva.
- Reprova: esse conhecimento ainda esta distribuido no codigo e na memoria do time.
- Risco principal: escalabilidade depender de quem lembra e nao do que esta escrito.
- Recomendacao objetiva: criar pagina interna simples para `StatusBadge`, `WorkspaceNotice`, `shell-surface` e derivados.

### 70. Lente de regressao visual
- Aprova: a interface tem consistencia suficiente para ser snapshotada com valor.
- Reprova: ainda nao ha trilha clara de validacao visual entre breakpoints e estados.
- Risco principal: um ajuste pequeno quebrar verdade, contraste ou densidade sem ser percebido.
- Recomendacao objetiva: adicionar smoke visual para home, users, agents e library.

## Conselho 8: psicologia cognitiva, carga mental e persuasao

### 71. BJ Fogg
- Aprova: a interface reduz barreiras iniciais de entendimento.
- Reprova: alguns gatilhos de acao ainda apontam para areas que nao entregam valor pleno.
- Risco principal: motivacao alta colidir com capacidade real baixa.
- Recomendacao objetiva: alinhar prompts e CTAs a acoes que o sistema sustenta de verdade.

### 72. Daniel Kahneman
- Aprova: o shell facilita leitura rapida e julgamento intuitivo.
- Reprova: sinais visuais elegantes demais podem induzir o Sistema 1 a superestimar maturidade funcional.
- Risco principal: heuristica de fluencia virar falsa confianca.
- Recomendacao objetiva: introduzir friccao informativa minima quando a area for preview ou dado parcial.

### 73. Amos Tversky
- Aprova: a interface organiza comparacoes de forma relativamente limpa.
- Reprova: a maneira como preview e live convivem ainda convida a ancoragens erradas.
- Risco principal: primeira impressao funcional enviesar toda a avaliacao posterior.
- Recomendacao objetiva: tornar o enquadramento inicial do produto mais preciso.

### 74. John Sweller
- Aprova: a interface diminuiu carga extranea em relacao a estruturas mais densas.
- Reprova: ainda existe carga germane desperdicada tentando decodificar o escopo real.
- Risco principal: energia mental ir para interpretar o sistema em vez de operar o sistema.
- Recomendacao objetiva: reduzir ambiguidade de estados e simplificar o caminho para a tarefa principal.

### 75. Aarron Walter
- Aprova: o produto possui personalidade visual e calor suficiente para parecer moderno.
- Reprova: a camada emocional ainda corre o risco de exceder a camada de confiabilidade.
- Risco principal: afeicao inicial seguida de decepcao operacional.
- Recomendacao objetiva: equilibrar charme visual com honestidade brutal sobre maturidade de cada area.

### 76. Kathy Sierra
- Aprova: o shell pode ajudar o usuario a parecer competente rapidamente.
- Reprova: essa promessa ainda depende demais de caminhos que nao estao todos maduros.
- Risco principal: o usuario nao se sentir mais capaz depois da exploracao.
- Recomendacao objetiva: concentrar energia em fluxos que aumentam competencia perceptivel de imediato.

### 77. Nir Eyal
- Aprova: ha bons gatilhos de entrada para explorar o workspace.
- Reprova: o loop de retorno de valor ainda nao esta fechado em busca e areas preview.
- Risco principal: curiosidade sem retencao funcional.
- Recomendacao objetiva: construir ciclos claros de acao, recompensa e retorno em areas live.

### 78. Antonio Damasio
- Aprova: a interface evoca seguranca e ordem.
- Reprova: o usuario ainda pode sentir conflito quando a emocao de controle encontra um erro ambiguo.
- Risco principal: dissonancia entre sentimento de dominio e resposta do sistema.
- Recomendacao objetiva: desenhar mensagens de erro que preservem senso de orientacao e agencia.

### 79. Richard Thaler
- Aprova: a arquitetura atual ja consegue `nudges` leves sem ser invasiva.
- Reprova: alguns nudges ainda empurram para exploracao antes de consolidar o que e util.
- Risco principal: incentivar comportamento que nao maximiza resultado do usuario.
- Recomendacao objetiva: usar nudges para levar primeiro ao valor real, depois ao restante.

### 80. Cass Sunstein
- Aprova: a interface esta perto de uma boa arquitetura de escolha.
- Reprova: ainda faltam guardrails mais claros entre opcao madura e opcao exploratoria.
- Risco principal: escolha mal enquadrada gerar interpretacao errada de prioridade.
- Recomendacao objetiva: diferenciar por padrao visual e textual o que e decisao segura e o que e experimental.

## Conselho 9: busca, findability, memoria e recuperacao de contexto

### 81. Marcia Bates
- Aprova: a busca ja serve como ponte entre navegacao e descoberta.
- Reprova: ainda nao apoia de verdade o berrypicking de um workspace real.
- Risco principal: o usuario encontrar rotas, mas nao encontrar informacao operacional de fato.
- Recomendacao objetiva: incluir origem, entidade e contexto nos resultados futuros.

### 82. Marti Hearst
- Aprova: a interface da busca e limpa e clara.
- Reprova: a recuperacao ainda e rasa e quase totalmente controlada por fixture local.
- Risco principal: boa experiencia de consulta sem boa experiencia de resposta.
- Recomendacao objetiva: evoluir o motor antes de sofisticar mais a casca.

### 83. Ricardo Baeza-Yates
- Aprova: existe uma taxonomia inicial utilizavel para indexacao.
- Reprova: ainda nao ha sinais de relevancia, cobertura ou frescor de resultado.
- Risco principal: busca perder credibilidade assim que o volume de informacao crescer.
- Recomendacao objetiva: definir desde ja metadados e ranking minimos para a indexacao futura.

### 84. Gary Marchionini
- Aprova: a busca apoia exploracao inicial do ambiente.
- Reprova: ainda nao ajuda o usuario a aprender progressivamente sobre o espaco de informacao.
- Risco principal: busca servir apenas para pular entre paginas.
- Recomendacao objetiva: usar a busca para ensinar estrutura, tipo e origem do conteudo encontrado.

### 85. Peter Pirolli
- Aprova: o produto ja emite algum `information scent`.
- Reprova: o cheiro da informacao ainda aponta mais para rota do que para resposta.
- Risco principal: navega-se bem, resolve-se pouco.
- Recomendacao objetiva: enriquecer resultados com contexto suficiente para decisao sem clique cego.

### 86. Lente de recuperacao contextual
- Aprova: o shell tem base visual para manter contexto entre busca e destino.
- Reprova: ainda nao ha memoria real de historico, origem ou ultimo estado util.
- Risco principal: cada busca parecer um novo comeco.
- Recomendacao objetiva: incorporar historico, recentidade e ancoragem contextual na experiencia de busca.

### 87. Lente de ranking semantico
- Aprova: os grupos atuais ajudam a organizar resultado bruto.
- Reprova: ainda nao existe hierarquia de relevancia baseada em intencao.
- Risco principal: todos os resultados parecerem igualmente bons.
- Recomendacao objetiva: priorizar resultados por tarefa, estado e frequencia de uso.

### 88. Lente de indexacao por entidades
- Aprova: a estrutura atual ja permite imaginar entidades como usuario, area, tarefa e contexto.
- Reprova: essas entidades ainda nao estao modeladas como base da recuperacao.
- Risco principal: a busca crescer em texto solto e nao em objetos de produto.
- Recomendacao objetiva: desenhar a indexacao futura em torno de entidades e relacionamentos.

### 89. Lente de busca por teclado
- Aprova: `Ctrl/Cmd+K`, Enter e Escape ja estao bem encaminhados.
- Reprova: ainda falta semantica perfeita para item ativo e potencial para navegacao mais rapida.
- Risco principal: parecer excelente no teclado para usuario visual e apenas razoavel para acessibilidade.
- Recomendacao objetiva: tratar busca por teclado como produto proprio dentro do shell.

### 90. Lente de search analytics
- Aprova: a busca ja tem definicao minima de superficie.
- Reprova: ainda nao mede nada sobre termo, falha, clique e abandono.
- Risco principal: o time nao saber se a busca ajuda, atrapalha ou simplesmente existe.
- Recomendacao objetiva: instrumentar evento de abertura, consulta, selecao, vazio e erro.

## Conselho 10: estrategia de produto, confianca e verdade operacional

### 91. April Dunford
- Aprova: o produto tem posicionamento visual distinto.
- Reprova: o posicionamento funcional ainda pode ser interpretado de forma ampla demais.
- Risco principal: mercado entender uma categoria de produto diferente da capacidade atual.
- Recomendacao objetiva: explicitar no produto o que o Lume faz hoje melhor do que os outros.

### 92. Julie Zhuo
- Aprova: o Lume ja tem uma experiencia com narrativa coerente.
- Reprova: ainda faltam pequenos detalhes de uso que separam um sistema polido de um sistema confiavel.
- Risco principal: impressao forte com detalhes quebrando o encanto no uso real.
- Recomendacao objetiva: tratar os pequenos atritos de estado e acessibilidade como prioridade de lideranca.

### 93. Gibson Biddle
- Aprova: a shell atual sustenta uma historia de produto mais clara.
- Reprova: ainda nao esta totalmente claro qual problema central o usuario deve associar ao Lume em primeiro lugar.
- Risco principal: amplitude sem foco.
- Recomendacao objetiva: definir a promessa principal do produto e fazer cada rota reforca-la.

### 94. Laura Klein
- Aprova: o sistema ja esta pronto para aprender com usuarios reais.
- Reprova: ainda depende demais de julgamento interno sobre clareza e valor.
- Risco principal: descoberta tardia de problemas que ja estao aparentes no uso.
- Recomendacao objetiva: validar com usuarios representativos antes de expandir modulos preview.

### 95. Hope Gurion
- Aprova: a interface ja pode sustentar experimento de onboarding leve.
- Reprova: ainda nao esta claro quais sinais indicam sucesso do usuario nos primeiros minutos.
- Risco principal: bom first impression sem first value.
- Recomendacao objetiva: desenhar a primeira sessao em torno de uma conquista concreta.

### 96. Leah Tharin
- Aprova: a direcao atual e pragmatica e pode gerar valor rapido.
- Reprova: o risco de feature theater ainda nao desapareceu.
- Risco principal: criar sensacao de ecossistema maior do que a profundidade do produto.
- Recomendacao objetiva: exigir justificativa de confianca para cada nova superficie antes de publica-la.

### 97. Shreyas Doshi
- Aprova: a interface ja permite escolhas mais maduras de priorizacao.
- Reprova: ainda falta radical clareza sobre o que o usuario deveria fazer agora e por que isso importa.
- Risco principal: boas paginas sem decisao de produto nitida.
- Recomendacao objetiva: transformar foco de produto em foco de interface.

### 98. Brian Balfour
- Aprova: a shell atual poderia apoiar loops de ativacao e retencao.
- Reprova: os loops ainda nao estao instrumentados nem fechados em valor.
- Risco principal: crescimento sem retencao de uso qualificado.
- Recomendacao objetiva: conectar areas live a metricas de ativacao antes de ampliar superficie.

### 99. Tomer Sharon
- Aprova: ha base suficiente para pesquisa qualitativa de alta qualidade.
- Reprova: o produto ainda nao mostra claramente onde termina a opiniao do time e comeca o aprendizado validado.
- Risco principal: o discurso de design ficar mais forte que a escuta do usuario.
- Recomendacao objetiva: programar pesquisa observacional curta apos o P0.

### 100. Lente de governanca de preview/live
- Aprova: o Lume ja reconhece que nem tudo esta live, e isso e raro e valioso.
- Reprova: essa governanca ainda nao esta escrita como regra de produto.
- Risco principal: futuras rotas repetirem ambiguidade ja identificada.
- Recomendacao objetiva: criar politica formal para qualquer superficie `preview`, `beta`, `live` ou `internal`.

## Fechamento do painel

As 100 vozes convergem em um ponto: o Lume nao precisa de outra troca radical de interface agora. Precisa de mais rigor em `verdade operacional`, `acessibilidade`, `findability`, `governanca de estados` e `maturidade de contratos`. O shell atual e bom o bastante para merecer esse investimento.
