import { FiActivity, FiBell, FiLayers, FiZap } from 'react-icons/fi';
import MetricCard from '../components/common/MetricCard';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { humanizeChargebackMode, humanizeToken } from '../utils/uiText';

export default function Usage() {
  const { session, usage } = useShell();
  const budget = usage?.budget;
  const canManageBudgets = session?.role.permissions.includes('budgets.manage') ?? false;

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Uso e limites"
        description="Acompanhe consumo, creditos e guardrails do workspace."
        state="live"
        detail={usage?.note}
      />

      <section className="grid gap-4 lg:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={FiZap}
          label="Creditos restantes"
          value={usage ? `${usage.remainingCredits}/${usage.dailyCredits}` : '--'}
          detail="Leitura operacional atual do workspace."
          state="live"
        />
        <MetricCard
          icon={FiActivity}
          label="Consumo"
          value={usage?.consumedCredits ?? '--'}
          detail="Consumo total registrado neste workspace."
          state="live"
        />
        <MetricCard
          icon={FiLayers}
          label="Tarefas ativas"
          value={usage?.activeTasks ?? '--'}
          detail="Tarefas em andamento neste workspace."
          state="live"
        />
        <MetricCard
          icon={FiBell}
          label="Notificacoes"
          value={usage?.unreadNotifications ?? '--'}
          detail="Atualizacoes que ainda precisam de leitura."
          state="live"
        />
      </section>

      <section className="grid gap-4 lg:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={FiLayers}
          label="Limite de alerta"
          value={budget ? `${budget.consumedCredits}/${budget.softLimitCredits}` : '--'}
          detail="Faixa de alerta para acompanhar o consumo."
          state={budget?.softLimitReached ? 'preview' : 'live'}
        />
        <MetricCard
          icon={FiLayers}
          label="Limite maximo"
          value={budget ? `${budget.consumedCredits}/${budget.hardLimitCredits}` : '--'}
          detail="Limite maximo permitido para o workspace."
          state={budget?.hardLimitReached ? 'disabled-preview' : 'live'}
        />
        <MetricCard
          icon={FiActivity}
          label="Centro de custo"
          value={budget?.costCenter ?? '--'}
          detail={`${budget ? humanizeChargebackMode(budget.chargebackMode) : 'Showback'} | ${budget?.budgetStatus ? humanizeToken(budget.budgetStatus) : 'Saudavel'}`}
          state="live"
        />
        <MetricCard
          icon={FiZap}
          label="Gestao"
          value={canManageBudgets ? 'Pode editar' : 'Somente leitura'}
          detail="Mostra quem pode ajustar limites e politicas."
          state="live"
        />
      </section>

      {budget ? (
        <section className="shell-panel p-5">
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Resumo de uso</p>
          <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">{budget.note}</p>
          <div className="mt-4 grid gap-3 lg:grid-cols-3">
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Rateio do consumo</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{humanizeChargebackMode(budget.chargebackMode)}</p>
            </div>
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso do limite de alerta</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.softLimitUtilizationPercent}%</p>
            </div>
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso do limite maximo</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.hardLimitUtilizationPercent}%</p>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}
