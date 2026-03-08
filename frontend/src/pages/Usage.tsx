import { FiActivity, FiBell, FiLayers, FiZap } from 'react-icons/fi';
import MetricCard from '../components/common/MetricCard';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';

export default function Usage() {
  const { usage } = useShell();

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
    </div>
  );
}
