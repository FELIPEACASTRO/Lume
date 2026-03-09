import { FiActivity, FiBell, FiLayers, FiZap } from 'react-icons/fi';
import MetricCard from '../components/common/MetricCard';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';

export default function Usage() {
  const { session, usage } = useShell();
  const budget = usage?.budget;
  const canManageBudgets = session?.role.permissions.includes('budgets.manage') ?? false;

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Uso operacional do workspace agora entra na shell."
        description="A Lume absorve a ideia de creditos como metering e limite operacional, sem reposicionar o produto para consumo individual freemium."
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
          detail="Volume agregado para a shell e a task view."
          state="live"
        />
        <MetricCard
          icon={FiLayers}
          label="Tarefas ativas"
          value={usage?.activeTasks ?? '--'}
          detail="Tarefas persistidas no backend neste workspace."
          state="live"
        />
        <MetricCard
          icon={FiBell}
          label="Notificacoes"
          value={usage?.unreadNotifications ?? '--'}
          detail="Eventos ainda nao lidos no inbox operacional."
          state="live"
        />
      </section>

      <section className="grid gap-4 lg:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={FiLayers}
          label="Soft limit"
          value={budget ? `${budget.consumedCredits}/${budget.softLimitCredits}` : '--'}
          detail="Budget operacional para alerta preventivo."
          state={budget?.softLimitReached ? 'preview' : 'live'}
        />
        <MetricCard
          icon={FiLayers}
          label="Hard limit"
          value={budget ? `${budget.consumedCredits}/${budget.hardLimitCredits}` : '--'}
          detail="Guardrail do workspace para operacao controlada."
          state={budget?.hardLimitReached ? 'disabled-preview' : 'live'}
        />
        <MetricCard
          icon={FiActivity}
          label="Cost center"
          value={budget?.costCenter ?? '--'}
          detail={`${budget?.chargebackMode ?? 'showback'} | ${budget?.budgetStatus ?? 'healthy'}`}
          state="live"
        />
        <MetricCard
          icon={FiZap}
          label="Governanca"
          value={canManageBudgets ? 'Admin' : 'Read-only'}
          detail="Budgets agora entram no uso operacional por workspace."
          state="live"
        />
      </section>

      {budget ? (
        <section className="shell-panel p-5">
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">FinOps operacional</p>
          <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">{budget.note}</p>
          <div className="mt-4 grid gap-3 lg:grid-cols-3">
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Showback/chargeback</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.chargebackMode}</p>
            </div>
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Utilizacao soft</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.softLimitUtilizationPercent}%</p>
            </div>
            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Utilizacao hard</p>
              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.hardLimitUtilizationPercent}%</p>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}
