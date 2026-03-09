import { FormEvent, useEffect, useMemo, useState } from 'react';
import { FiAlertCircle, FiArrowRight, FiLayers, FiPlus, FiSearch, FiUsers } from 'react-icons/fi';
import { Link, useNavigate } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { toApiClientError } from '../services/api';
import { homeService } from '../services/homeService';
import { taskService } from '../services/taskService';
import { HomeOverviewDto } from '../types';

export default function Home() {
  const navigate = useNavigate();
  const { openSearch, taskTypes, navigation, refreshSummary } = useShell();
  const [prompt, setPrompt] = useState('');
  const [selectedTaskType, setSelectedTaskType] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [overview, setOverview] = useState<HomeOverviewDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedTaskType && taskTypes.length > 0) {
      setSelectedTaskType(taskTypes[0].taskType);
    }
  }, [selectedTaskType, taskTypes]);

  const activeTaskType = useMemo(
    () => taskTypes.find((taskType) => taskType.taskType === selectedTaskType) ?? taskTypes[0] ?? null,
    [selectedTaskType, taskTypes]
  );

  const loadOverview = async () => {
    try {
      setLoading(true);
      setError(null);
      setOverview(await homeService.getOverview());
    } catch (loadError) {
      setOverview(null);
      setError(toApiClientError(loadError).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadOverview();
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextPrompt = prompt.trim();
    if (!nextPrompt) {
      setSubmitError('Descreva a tarefa antes de continuar.');
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError(null);
      const createdTask = await taskService.create({
        prompt: nextPrompt,
        taskType: selectedTaskType || activeTaskType?.taskType || 'research',
      });
      setPrompt('');
      await Promise.all([refreshSummary(), loadOverview()]);
      navigate(`/tasks/${createdTask.task.id}`);
    } catch (loadError) {
      setSubmitError(toApiClientError(loadError).message);
    } finally {
      setSubmitting(false);
    }
  };

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (!overview) {
      return 'empty';
    }
    return 'live';
  }, [error, loading, overview]);

  const quickLinks = navigation.filter((item) => ['tasks', 'projects', 'library', 'users', 'settings'].includes(item.id));

  return (
    <div className="page-frame flex flex-col gap-8 pb-10">
      <section className="mx-auto flex max-w-4xl flex-col items-center text-center">
        <span
          className="rounded-full border px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.24em]"
          style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-secondary)' }}
        >
          Inicio
        </span>
        <h1 className="mt-6 max-w-4xl text-4xl leading-tight text-[var(--text-primary)] sm:text-5xl lg:text-6xl" style={{ fontFamily: 'var(--font-display)' }}>
          O que voce quer fazer?
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--text-secondary)] sm:text-lg">
          Registre uma tarefa, acompanhe o que esta em andamento e abra os pontos que pedem atencao.
        </p>
      </section>

      <WorkspaceNotice
        title="Seu centro de comando"
        description="Use esta tela para abrir trabalho novo, acompanhar alertas e retomar o que ficou pendente."
        state="live"
        detail={overview?.supportingText}
      />

      <section className="manus-composer mx-auto w-full max-w-5xl p-5 sm:p-6">
        <form onSubmit={handleSubmit}>
          <textarea
            aria-label="Nova tarefa"
            className="min-h-[220px] w-full resize-none border-none bg-transparent text-lg leading-8 outline-none placeholder:text-[var(--text-tertiary)]"
            placeholder="Descreva o trabalho que voce quer abrir."
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
          />

          <div className="mt-5 rounded-[16px] border px-4 py-3" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
            <div className="flex flex-wrap items-center gap-2">
              <button type="button" className="icon-button" aria-label="Adicionar contexto">
                <FiPlus size={16} />
              </button>

              <button type="submit" className="btn-primary ml-auto" disabled={submitting}>
                {submitting ? 'Abrindo...' : 'Abrir tarefa'}
                <FiArrowRight size={16} />
              </button>
            </div>
          </div>

          <div className="mt-5 flex flex-wrap gap-3">
            {taskTypes.map((taskType) => (
              <button
                key={taskType.taskType}
                type="button"
                className="pill-button"
                style={selectedTaskType === taskType.taskType ? { borderColor: 'var(--surface-border-strong)', background: 'var(--fill-tsp-white-dark)' } : undefined}
                onClick={() => setSelectedTaskType(taskType.taskType)}
              >
                {taskType.label}
              </button>
            ))}
          </div>

          <div className="mt-6 flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between" style={{ borderColor: 'var(--surface-border-main)' }}>
            <div className="flex flex-wrap gap-3">
              <button type="button" className="btn-secondary" onClick={openSearch}>
                <FiSearch size={16} />
                Buscar
              </button>
              <Link to="/projects" className="btn-secondary">
                <FiLayers size={16} />
                Projetos
              </Link>
            </div>

            <p className="text-sm text-[var(--text-secondary)]">
              Tipo selecionado: <span className="font-semibold text-[var(--text-primary)]">{activeTaskType?.label ?? 'Pesquisar'}</span>
            </p>
          </div>
        </form>

        {submitError ? <p className="mt-4 text-sm font-medium text-[#df7d77]">{submitError}</p> : null}
      </section>

      <AsyncState
        state={state}
        loadingLabel="Carregando painel inicial..."
        errorTitle="Nao foi possivel montar a tela inicial."
        errorDescription="Tente atualizar. Se o erro continuar, revise a conexao e tente novamente."
        errorDetail={error ?? undefined}
        onRetry={() => void loadOverview()}
        emptyTitle="Ainda nao ha atividade suficiente."
        emptyDescription="Abra a primeira tarefa ou organize um projeto para preencher este painel."
      >
        {overview ? (
          <>
            <section className="grid gap-5 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
              <div className="shell-surface p-6">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Em andamento</p>
                    <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">O que precisa de voce agora</h2>
                  </div>
                  <Link to="/tasks" className="btn-secondary px-4">
                    Ver tarefas
                  </Link>
                </div>

                <div className="mt-5 space-y-3">
                  {overview.inProgress.length === 0 ? (
                    <div className="rounded-[12px] border px-5 py-5 text-sm leading-6 text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                      Nenhuma tarefa esta em andamento neste momento.
                    </div>
                  ) : (
                    overview.inProgress.map((item) => (
                      <Link
                        key={item.id}
                        to={item.path}
                        className="block rounded-[12px] border px-5 py-5 transition-all duration-200 hover:-translate-y-0.5"
                        style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-lg font-semibold text-[var(--text-primary)]">{item.title}</p>
                          <span className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{item.statusLabel}</span>
                        </div>
                        <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.summary}</p>
                        <p className="mt-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
                          {item.statusLabel} . {item.ownerName}
                        </p>
                      </Link>
                    ))
                  )}
                </div>
              </div>

              <div className="space-y-4">
                <div className="shell-surface p-6">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Alertas</p>
                  <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">O que mudou</h2>
                  <div className="mt-4 space-y-3">
                    {overview.alerts.length === 0 ? (
                      <p className="text-sm leading-6 text-[var(--text-secondary)]">Nenhum alerta novo no momento.</p>
                    ) : (
                      overview.alerts.map((alert) => (
                        <Link
                          key={alert.id}
                          to={alert.path}
                          className="block rounded-[12px] border px-4 py-4"
                          style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                        >
                          <div className="flex items-start gap-3">
                            <FiAlertCircle size={16} className="mt-0.5 text-[var(--accent)]" />
                            <div>
                              <p className="text-sm font-semibold text-[var(--text-primary)]">{alert.title}</p>
                              <p className="mt-1 text-sm leading-6 text-[var(--text-secondary)]">{alert.body}</p>
                              <p className="mt-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{alert.createdAt}</p>
                            </div>
                          </div>
                        </Link>
                      ))
                    )}
                  </div>
                </div>

                <div className="shell-surface p-6">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Equipe e contexto</p>
                  <div className="mt-4 grid gap-3">
                    {overview.teamAndContext.map((item) => (
                      <Link
                        key={item.id}
                        to={item.path ?? '/'}
                        className="rounded-[12px] border px-4 py-4"
                        style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                      >
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{item.label}</p>
                        <p className="mt-2 text-lg font-semibold text-[var(--text-primary)]">{item.headline}</p>
                        <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.description}</p>
                      </Link>
                    ))}
                  </div>
                </div>
              </div>
            </section>

            <section className="grid gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
              <div className="shell-surface p-6">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Recentes</p>
                    <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">Volte para onde parou</h2>
                  </div>
                </div>

                <div className="mt-5 grid gap-3">
                  {overview.recentItems.map((item) => (
                    <Link
                      key={item.id}
                      to={item.path}
                      className="rounded-[12px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                    >
                      <p className="text-base font-semibold text-[var(--text-primary)]">{item.title}</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.summary}</p>
                      <p className="mt-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{item.detail}</p>
                    </Link>
                  ))}
                </div>
              </div>

              <div className="shell-surface p-6">
                <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Acesso rapido</p>
                <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">Abra as areas principais</h2>
                <div className="mt-5 grid gap-3">
                  {quickLinks.map((item) => (
                    <Link
                      key={item.id}
                      to={item.path}
                      className="rounded-[12px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                    >
                      <p className="text-base font-semibold text-[var(--text-primary)]">{item.label}</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.description}</p>
                    </Link>
                  ))}
                  <button type="button" className="btn-secondary justify-center" onClick={openSearch}>
                    <FiSearch size={16} />
                    Abrir busca
                  </button>
                  <Link to="/users" className="btn-secondary justify-center">
                    <FiUsers size={16} />
                    Ver equipe
                  </Link>
                </div>
              </div>
            </section>
          </>
        ) : null}
      </AsyncState>
    </div>
  );
}
