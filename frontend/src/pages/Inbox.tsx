import { useEffect, useMemo, useState } from 'react';
import { FiArrowUpRight, FiBell } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { notificationService } from '../services/notificationService';
import { toApiClientError } from '../services/api';
import { NotificationDto } from '../types';

export default function Inbox() {
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      setError(null);
      setNotifications(await notificationService.findAll());
    } catch (loadError) {
      setError(toApiClientError(loadError).message);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadNotifications();
  }, []);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (notifications.length === 0) {
      return 'empty';
    }
    return 'live';
  }, [error, loading, notifications.length]);

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Inbox operacional do workspace."
        description="A shell agora tem notificacoes reais para revisao de tarefas, sincronizacao de biblioteca e eventos do modulo de agents."
        state="live"
        detail="Esta superficie prepara o caminho para activity feed, mentions e alertas mais profundos nas proximas fases."
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando inbox..."
        errorTitle="O inbox nao respondeu."
        errorDescription="As notificacoes do workspace nao foram carregadas."
        errorDetail={error ?? undefined}
        onRetry={() => void loadNotifications()}
        emptyTitle="Nenhuma notificacao encontrada."
        emptyDescription="Quando o workspace registrar eventos relevantes, eles vao aparecer aqui."
      >
        <section className="space-y-4">
          {notifications.map((notification) => (
            <article key={notification.id} className="shell-surface p-6">
              <div className="flex items-start gap-4">
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--ink-strong)]">
                  <FiBell size={18} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <p className="text-lg font-semibold text-[var(--ink-strong)]">{notification.title}</p>
                    {!notification.read ? (
                      <span className="rounded-full bg-[rgba(236,147,14,0.16)] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-[#6b4a08]">
                        Nova
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">{notification.body}</p>
                  <div className="mt-4 flex flex-wrap items-center gap-4 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                    <span>{notification.kind}</span>
                    <span>{notification.createdAt}</span>
                  </div>
                </div>
                <Link to={notification.path} className="btn-secondary shrink-0">
                  Abrir
                  <FiArrowUpRight size={16} />
                </Link>
              </div>
            </article>
          ))}
        </section>
      </AsyncState>
    </div>
  );
}
