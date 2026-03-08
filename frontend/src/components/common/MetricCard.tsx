import { IconType } from 'react-icons';
import { WorkspaceDataState } from '../../types';
import StatusBadge from './StatusBadge';

interface MetricCardProps {
  icon: IconType;
  label: string;
  value: string | number;
  detail: string;
  state: WorkspaceDataState;
}

export default function MetricCard({ icon: Icon, label, value, detail, state }: MetricCardProps) {
  return (
    <div className="manus-banner">
      <div className="flex items-start gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--accent)' }}>
          <Icon size={18} />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--text-secondary)]">
              {label}
            </p>
            <StatusBadge state={state} />
          </div>
          <p className="mt-3 text-3xl font-semibold text-[var(--text-primary)]">{value}</p>
          <p className="mt-2 text-sm text-[var(--text-secondary)]">{detail}</p>
        </div>
      </div>
    </div>
  );
}
