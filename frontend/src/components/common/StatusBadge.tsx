import { WorkspaceDataState } from '../../types';

interface StatusBadgeProps {
  state: WorkspaceDataState;
  className?: string;
}

const statusCopy: Record<WorkspaceDataState, string> = {
  live: 'Ativo',
  attention: 'Atencao',
  unavailable: 'Indisponivel',
  restricted: 'Restrito',
  loading: 'Carregando',
  empty: 'Sem dados',
  error: 'Erro',
};

export default function StatusBadge({ state, className = '' }: StatusBadgeProps) {
  return (
    <span className={['status-badge', `status-badge--${state}`, className].filter(Boolean).join(' ')}>
      {statusCopy[state]}
    </span>
  );
}
