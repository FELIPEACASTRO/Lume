import { ReactNode } from 'react';
import { WorkspaceDataState } from '../../types';
import StatusBadge from './StatusBadge';

interface WorkspaceNoticeProps {
  title: string;
  description: string;
  state: WorkspaceDataState;
  detail?: string;
  action?: ReactNode;
}

export default function WorkspaceNotice({
  title,
  description,
  state,
  detail,
  action,
}: WorkspaceNoticeProps) {
  return (
    <section className="manus-banner">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge state={state} />
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--text-secondary)]">
              Situacao
            </p>
          </div>
          <h2 className="mt-4 text-xl font-semibold text-[var(--text-primary)]">{title}</h2>
          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{description}</p>
          {detail ? <p className="mt-3 text-sm font-medium text-[var(--text-primary)]">{detail}</p> : null}
        </div>
        {action ? <div className="shrink-0">{action}</div> : null}
      </div>
    </section>
  );
}
