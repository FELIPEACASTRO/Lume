# Matriz de Conteudo da UX do Lume

## Objetivo
- Simplificar a linguagem do produto para operadores B2B.
- Tirar a interface do vocabulario interno de arquitetura.
- Garantir que cada tela tenha um objetivo claro, uma acao principal e mensagens de recuperacao objetivas.

## Voz da marca
- Direta
- Profissional
- Clara
- Sem jargao tecnico desnecessario

## Estrutura principal

| Area | Titulo | Objetivo | CTA principal | CTA secundaria |
| --- | --- | --- | --- | --- |
| Inicio | O que voce quer fazer? | Abrir trabalho novo e retomar pendencias | Abrir tarefa | Buscar |
| Tarefas | Tarefas | Acompanhar execucao e prioridades | Nova tarefa | Filtrar tarefas |
| Projetos | Projetos | Organizar contexto, responsaveis e prioridades | Novo projeto | Ver tarefas |
| Biblioteca | Arquivos e entregas | Consultar arquivos, versoes e historico | Registrar versao | Buscar |
| Equipe | Equipe | Gerir acessos e papeis | Convidar pessoa | Ajustar funcao |
| Configuracoes | Configuracoes do workspace | Ajustar preferencias, limites, conhecimento e provedores | Salvar alteracoes | Ver docs |

## Regras de copy
- Evitar termos como `shell`, `runtime`, `preview honesto`, `catalog-only`, `backend-first` na UI principal.
- Preferir verbos de acao:
  - `Abrir tarefa`
  - `Salvar configuracao`
  - `Ver tarefas`
  - `Registrar versao`
- Mensagens de erro devem dizer:
  - o que falhou
  - o que o usuario pode fazer
  - quando tentar novamente

## Estados padronizados
- `Ativo`: fluxo disponivel para uso.
- `Atencao`: algo precisa de revisao, mas o trabalho pode continuar.
- `Indisponivel`: area ou capacidade ainda nao pode ser usada.
- `Restrito`: visivel apenas para perfis autorizados.

## Empty states
- Inicio: `Abra a primeira tarefa ou organize um projeto para preencher este painel.`
- Tarefas: `Nenhuma tarefa encontrada com os filtros atuais.`
- Projetos: `Crie o primeiro projeto para organizar o trabalho do workspace.`
- Biblioteca: `Ainda nao ha arquivos ou entregas registrados.`
- Equipe: `Nenhum membro encontrado para este filtro.`
- Configuracoes: `Nenhuma configuracao disponivel para este perfil.`

## Erros
- Inicio: `Nao foi possivel montar a tela inicial. Tente atualizar.`
- Tarefas: `Nao foi possivel carregar as tarefas. Tente novamente.`
- Biblioteca: `Nao foi possivel carregar os arquivos e entregas.`
- Configuracoes: `Nao foi possivel carregar as configuracoes.`

## Observacoes
- Modulos incompletos ficam fora da navegacao principal.
- Areas restritas aparecem apenas com permissao e estado real no backend.
