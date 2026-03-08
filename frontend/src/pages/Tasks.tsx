import { useCallback, useEffect, useMemo, useState } from 'react';
import { FiArrowRight, FiCalendar, FiFolder, FiShare2, FiZap } from 'react-icons/fi';
import { Link, useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { toApiClientError } from '../services/api';
import { taskService } from '../services/taskService';
import { TaskSummaryDto } from '../types';

export default function Tasks() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('project') ?? undefined;
  const [tasks, setTasks] = useState<TaskSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTasks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setTasks(await taskService.findAll(projectId));
    } catch (loadError) {
      setError(toApiClientError(loadError).message);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (tasks.length === 0) {
      return 'empty';
    }
    return 'live';
  }, [error, loading, tasks.length]);

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Task board inspirado no Manus, mas preso aos contratos reais."
        description="A visualizacao agora trata cada task como uma trilha de execucao assistida. O que ainda nao tem backend, como follow-up ou refresh de plano, aparece como preview rotulado."
        state="preview"
        detail="Cada tarefa continua nascendo no backend e entrando na busca global, no historico do shell e no backlog do workspace."
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando task board..."
        errorTitle="As tarefas nao responderam."
        errorDescription="O backend do workspace nao retornou a lista de tarefas esperada."
        errorDetail={error ?? undefined}
        onRetry={() => void loadTasks()}
        emptyTitle="Nenhuma tarefa persistida ainda."
        emptyDescription="Crie uma nova tarefa a partir da home para inaugurar o board."
      >
        <section className="grid gap-4 xl:grid-cols-2">
          {tasks.map((task) => (
            <article key={task.id} className="shell-surface p-6">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--accent)' }}>
                    <FiFolder size={18} />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-lg font-semibold text-[var(--text-primary)]">{task.title}</p>
                      <StatusBadge state={task.availability} />
                    </div>
                    <p className="mt-2 text-sm font-medium text-[var(--text-secondary)]">
                      {task.taskType}
                      {task.projectName ? ` . ${task.projectName}` : ''}
                    </p>
                    <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{task.summary}</p>
                  </div>
                </div>

                <div className="rounded-full border px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.18em]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-secondary)' }}>
                  {task.statusLabel}
                </div>
              </div>

              <div className="mt-5 grid gap-3 rounded-[12px] border px-4 py-4 sm:grid-cols-3" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">Quando</p>
                  <div className="mt-2 flex items-center gap-2 text-sm text-[var(--text-secondary)]">
                    <FiCalendar size={14} />
                    {task.scheduledFor ?? task.updatedAt}
                  </div>
                </div>
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">Owner</p>
                  <p className="mt-2 text-sm text-[var(--text-primary)]">{task.ownerName}</p>
                </div>
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">Acoes</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    <span className="rounded-full border px-2 py-1 text-[11px] font-semibold" style={{ borderColor: 'var(--surface-border-main)', color: 'var(--text-secondary)' }}>
                      <FiZap className="inline-block" size={12} /> Refresh plan
                    </span>
                    <span className="rounded-full border px-2 py-1 text-[11px] font-semibold" style={{ borderColor: 'var(--surface-border-main)', color: 'var(--text-disabled)' }}>
                      <FiShare2 className="inline-block" size={12} /> Preview
                    </span>
                  </div>
                </div>
              </div>

              <div className="mt-5 flex flex-wrap gap-3">
                <Link to={`/tasks/${task.id}`} className="btn-primary">
                  Abrir task view
                  <FiArrowRight size={16} />
                </Link>
                {task.shareSlug ? (
                  <button type="button" className="btn-secondary" title="Compartilhamento visivel, mas ainda sem acao publica final">
                    <FiShare2 size={16} />
                    Compartilhar
                  </button>
                ) : (
                  <button type="button" className="btn-secondary" disabled title="Aparece como preview ate ganhar contrato real">
                    <FiShare2 size={16} />
                    Compartilhar
                  </button>
                )}
              </div>
            </article>
          ))}
        </section>
      </AsyncState>
    </div>
  );
}
