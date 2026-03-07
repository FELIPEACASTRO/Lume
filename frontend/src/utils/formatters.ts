/**
 * Utilitários de formatação reutilizáveis.
 * Princípio SRP: funções puras responsáveis apenas por formatação.
 */

/**
 * Formata uma string de data ISO para o formato brasileiro (dd/mm/aaaa).
 */
export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

/**
 * Formata uma string de data ISO para formato completo com hora.
 */
export function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
