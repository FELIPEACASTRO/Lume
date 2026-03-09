export function humanizeToken(value?: string | null): string {
  if (!value) {
    return '--';
  }

  const normalized = value.trim().toLowerCase();
  const dictionary: Record<string, string> = {
    configured: 'Acesso pronto',
    missing_credentials: 'Credenciais pendentes',
    execution_supported: 'Execucao disponivel',
    manual_only: 'Uso manual',
    live: 'Ativo',
    ready: 'Pronto',
    partial: 'Parcial',
    catalog_only: 'Catalogo',
    implemented_with_restrictions: 'Com restricoes',
    integration_verified: 'Validado',
    offline_verified: 'Validacao interna',
    online_verified: 'Validacao online',
    unsupported: 'Indisponivel',
    static: 'Estatico',
    last_connectivity_test: 'Ultimo teste',
    memory: 'Memoria',
    durable: 'Persistente',
    high_roi: 'Alta prioridade',
    sync: 'Sincrono',
    async: 'Assincrono',
    text_runtime: 'Texto',
    research_search: 'Pesquisa',
    media_audio: 'Midia',
    threat_intel: 'Threat intel',
    showback: 'Showback',
    chargeback: 'Chargeback',
    draft: 'Rascunho',
    queued: 'Na fila',
    ready_for_execution: 'Pronta',
    running: 'Em andamento',
    completed: 'Concluida',
    failed: 'Com erro',
    healthy: 'Saudavel',
    primary_docs: 'Documentacao oficial',
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
