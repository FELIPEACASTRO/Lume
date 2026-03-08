import { ReactNode } from 'react';
import EmptyState from './EmptyState';
import Loading from './Loading';
import WorkspaceNotice from './WorkspaceNotice';

interface AsyncStateProps {
  state: 'loading' | 'error' | 'empty' | 'live';
  loadingLabel?: string;
  errorTitle?: string;
  errorDescription?: string;
  errorDetail?: string;
  onRetry?: () => void;
  emptyTitle?: string;
  emptyDescription?: string;
  emptyActionLabel?: string;
  onEmptyAction?: () => void;
  children: ReactNode;
}

export default function AsyncState({
  state,
  loadingLabel,
  errorTitle,
  errorDescription,
  errorDetail,
  onRetry,
  emptyTitle,
  emptyDescription,
  emptyActionLabel,
  onEmptyAction,
  children,
}: AsyncStateProps) {
  if (state === 'loading') {
    return <Loading label={loadingLabel} />;
  }

  if (state === 'error') {
    return (
      <WorkspaceNotice
        title={errorTitle ?? 'Nao foi possivel carregar os dados.'}
        description={errorDescription ?? 'O backend nao respondeu como esperado.'}
        state="error"
        detail={errorDetail}
        action={
          onRetry ? (
            <button type="button" className="btn-primary" onClick={onRetry}>
              Tentar novamente
            </button>
          ) : undefined
        }
      />
    );
  }

  if (state === 'empty') {
    return (
      <EmptyState
        title={emptyTitle ?? 'Nenhum dado disponivel'}
        description={emptyDescription ?? 'Ainda nao ha conteudo para exibir nesta area.'}
        actionLabel={emptyActionLabel}
        onAction={onEmptyAction}
      />
    );
  }

  return <>{children}</>;
}
