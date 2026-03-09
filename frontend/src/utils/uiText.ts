export function humanizeToken(value?: string | null): string {
  if (!value) {
    return '--';
  }

  const normalized = value.trim().toLowerCase();
  const dictionary: Record<string, string> = {
    configured: 'Acesso pronto',
    attention: 'Atencao',
    unavailable: 'Indisponivel',
    missing_credentials: 'Credenciais pendentes',
    execution_supported: 'Execucao disponivel',
    manual_only: 'Uso manual',
    live: 'Ativo',
    ready: 'Pronto',
    partial: 'Parcial',
    catalog_only: 'Planejado',
    implemented_with_restrictions: 'Parcial',
    integration_verified: 'Conexao validada',
    offline_verified: 'Validacao interna',
    online_verified: 'Validacao online',
    unsupported: 'Indisponivel',
    static: 'Estatico',
    last_connectivity_test: 'Ultimo teste',
    memory: 'Temporario',
    durable: 'Persistente',
    high_roi: 'Alta prioridade',
    sync: 'Resposta imediata',
    async: 'Em fila',
    text_runtime: 'Texto',
    research_search: 'Pesquisa web',
    media_audio: 'Midia e voz',
    threat_intel: 'Monitoramento restrito',
    showback: 'Showback',
    chargeback: 'Chargeback',
    draft: 'Rascunho',
    queued: 'Na fila',
    ready_for_execution: 'Pronta',
    running: 'Em andamento',
    completed: 'Concluida',
    failed: 'Com erro',
    healthy: 'Saudavel',
    primary_docs: 'Fonte oficial',
    responses: 'Respostas',
    messages: 'Mensagens',
    'generate-content': 'Resposta direta',
    generate_content: 'Resposta direta',
    'chat-completions': 'Chat',
    chat_completions: 'Chat',
    'chat-v2': 'Chat',
    chat_v2: 'Chat',
    'openai-compat': 'Compatibilidade OpenAI',
    openai_compat: 'Compatibilidade OpenAI',
    'embeddings-rerank': 'Busca semantica',
    embeddings_rerank: 'Busca semantica',
    prototype: 'Prototipo',
  };

  if (dictionary[normalized]) {
    return dictionary[normalized];
  }

  return value
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

export function humanizeRuntimeState(value?: string | null): string {
  return humanizeToken(value);
}

export function humanizeChargebackMode(value?: string | null): string {
  return humanizeToken(value);
}
