import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  FiArrowUpRight,
  FiCheckCircle,
  FiClock,
  FiShare2,
} from 'react-icons/fi';
import { Link, useParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { toApiClientError } from '../services/api';
import { taskService } from '../services/taskService';
import { TaskDetailDto } from '../types';

function stepIcon(statusLabel: string) {
  if (statusLabel === 'completed') {
    return <FiCheckCircle className="text-[var(--accent)]" size={16} />;
  }
  return <FiClock className="text-[var(--text-secondary)]" size={16} />;
}

export default function TaskView() {
  const { taskId = '' } = useParams();
  const [detail, setDetail] = useState<TaskDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTask = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setDetail(await taskService.findById(taskId));
    } catch (loadError) {
      setError(toApiClientError(loadError).message);
      setDetail(null);
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    void loadTask();
  }, [loadTask]);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (!detail) {
      return 'empty';
    }
    return 'live';
  }, [detail, error, loading]);

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Task view com estado real."
        description="A tarefa, os steps e o resumo abaixo refletem apenas o pipeline e os dados persistidos no backend."
        state="live"
        detail="Quando nao houver steps, sugestoes ou compartilhamento, a tela informa essa ausencia em vez de simular recursos."
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando task view..."
        errorTitle="Nao foi possivel abrir a tarefa."
        errorDescription="O backend do workspace nao retornou a task view solicitada."
        errorDetail={error ?? undefined}
        onRetry={() => void loadTask()}
        emptyTitle="A tarefa nao foi encontrada."
        emptyDescription="Verifique se o identificador ainda existe ou volte para a lista de tarefas."
      >
        {detail ? (
          <section className="shell-surface overflow-hidden">
            <div className="border-b px-6 py-6 sm:px-7" style={{ borderColor: 'var(--surface-border-main)' }}>
              <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{detail.task.taskType}</p>
                    <StatusBadge state={detail.task.availability} />
                  </div>
                  <h1 className="mt-3 text-3xl font-semibold text-[var(--text-primary)]">{detail.task.title}</h1>
                  <p className="mt-3 max-w-3xl text-sm leading-7 text-[var(--text-secondary)]">{detail.task.prompt}</p>
                </div>

                <div className="flex flex-wrap gap-3">
                  <div className="rounded-full border px-4 py-2 text-sm font-semibold" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}>
                    {detail.task.statusLabel}
                  </div>
                  <button type="button" className="btn-secondary" disabled={!detail.task.shareSlug}>
                    <FiShare2 size={16} />
                    Compartilhar
                  </button>
                </div>
              </div>
            </div>

            <div className="grid gap-6 px-6 py-6 sm:px-7 xl:grid-cols-[minmax(0,1.25fr)_minmax(0,0.75fr)]">
              <div className="space-y-4">
                {detail.steps.length === 0 ? (
                  <div className="rounded-[12px] border px-5 py-5 text-sm leading-6 text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                    Esta tarefa ainda nao possui steps persistidos.
                  </div>
                ) : detail.steps.map((step) => (
                  <div key={step.id} className="rounded-[12px] border px-5 py-5" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3">
                        <div className="mt-0.5">{stepIcon(step.statusLabel)}</div>
                        <div>
                          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{step.stepType}</p>
                          <p className="mt-2 text-base font-semibold text-[var(--text-primary)]">{step.title}</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{step.detail}</p>
                        </div>
                      </div>
                      <span className="rounded-full border px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em]" style={{ borderColor: 'var(--surface-border-main)', color: 'var(--text-secondary)' }}>
                        {step.statusLabel}
                      </span>
                    </div>
                  </div>
                ))}

                <div className="rounded-[12px] border px-5 py-5" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--accent)' }}>
                      <FiClock size={16} />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-[var(--text-primary)]">Mensagem do Lume</p>
                      <p className="text-sm text-[var(--text-secondary)]">Resumo operacional da tarefa atual.</p>
                    </div>
                  </div>
                  <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
                    {detail.task.summary}
                  </p>
                  {detail.task.lastError ? (
                    <p className="mt-4 text-sm font-medium text-[#df7d77]">{detail.task.lastError}</p>
                  ) : null}
                </div>
              </div>

              <aside className="space-y-4">
                <div className="shell-panel p-5">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Resumo</p>
                  <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">{detail.task.summary}</p>
                  <p className="mt-4 text-sm font-semibold text-[var(--text-primary)]">Owner: {detail.task.ownerName}</p>
                  <p className="mt-2 text-sm text-[var(--text-secondary)]">Atualizado em {detail.task.updatedAt}</p>
                  <p className="mt-2 text-sm text-[var(--text-secondary)]">Runtime: {detail.task.runtimeState}</p>
                </div>

                <div className="shell-panel p-5">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Sugestoes de acompanhamento</p>
                  <div className="mt-4 space-y-3">
                    {detail.followUpSuggestions.length === 0 ? (
                      <div className="rounded-[12px] border px-4 py-4 text-sm leading-6 text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)' }}>
                        Nenhuma sugestao derivada foi registrada para esta tarefa.
                      </div>
                    ) : detail.followUpSuggestions.map((suggestion) => (
                      <div key={suggestion} className="flex items-start gap-3 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)' }}>
                        <FiClock size={16} className="mt-0.5 text-[var(--accent)]" />
                        <p className="text-sm leading-6 text-[var(--text-primary)]">{suggestion}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <Link to="/tasks" className="btn-secondary w-full">
                  Voltar ao board
                  <FiArrowUpRight size={16} />
                </Link>
              </aside>
            </div>

          </section>
        ) : null}
      </AsyncState>
    </div>
  );
}
