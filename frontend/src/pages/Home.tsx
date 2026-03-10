import { FormEvent, useEffect, useMemo, useState } from 'react';
import { FiAlertCircle, FiArrowRight, FiLayers, FiPlus, FiSearch } from 'react-icons/fi';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { toApiClientError } from '../services/api';
import { homeService } from '../services/homeService';
import { taskService } from '../services/taskService';
import { HomeOverviewBlockDto, HomeOverviewDto } from '../types';

export default function Home() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { openSearch, taskTypes, navigation, refreshSummary } = useShell();
  const [prompt, setPrompt] = useState('');
  const [selectedTaskType, setSelectedTaskType] = useState('');
  const [mode, setMode] = useState<'search' | 'task'>(() => (searchParams.get('mode') === 'task' ? 'task' : 'search'));
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

  useEffect(() => {
    setMode(searchParams.get('mode') === 'task' ? 'task' : 'search');
  }, [searchParams]);

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
      setSubmitError(mode === 'search' ? 'Digite o que voce quer encontrar antes de buscar.' : 'Descreva a tarefa antes de continuar.');
      return;
    }

    if (mode === 'search') {
      setSubmitError(null);
      navigate(`/search/results?q=${encodeURIComponent(nextPrompt)}`);
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

  const handleModeChange = (nextMode: 'search' | 'task') => {
    setMode(nextMode);
    setSubmitError(null);
    if (nextMode === 'task') {
      setSearchParams({ mode: 'task' }, { replace: true });
      return;
    }
    setSearchParams({}, { replace: true });
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
  const orderedBlocks = useMemo(
    () => (overview?.blocks ?? []).filter((block) => block.enabled).sort((left, right) => left.sortOrder - right.sortOrder),
    [overview]
  );

  const blockItems = <T,>(items: T[], block: HomeOverviewBlockDto) => items.slice(0, block.maxItems ?? 4);

  const renderBlockAction = (block: HomeOverviewBlockDto, fallbackLabel?: string, fallbackPath?: string) => {
    const label = block.ctaLabel?.trim() || fallbackLabel;
    const path = block.ctaPath?.trim() || fallbackPath;
    if (!label || !path) {
      return null;
    }
    return (
      <Link to={path} className="btn-secondary px-4">
        {label}
      </Link>
    );
  };

  const renderBlock = (blockType: string) => {
    if (!overview) {
      return null;
    }
    const block = overview.blocks.find((item) => item.blockType === blockType && item.enabled);
    if (!block) {
      return null;
    }

    if (block.blockType === 'in_progress') {
      return (
        <article key={block.id} className="shell-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{block.title}</p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{block.description}</p>
            </div>
            {renderBlockAction(block, 'Ver tarefas', '/tasks')}
          </div>

          <div className="mt-5 space-y-3">
            {blockItems(overview.inProgress, block).length === 0 ? (
              <div className="rounded-[12px] border px-5 py-5 text-sm leading-6 text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                Nenhuma tarefa esta em andamento neste momento.
              </div>
            ) : (
              blockItems(overview.inProgress, block).map((item) => (
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
        </article>
      );
    }

    if (block.blockType === 'alerts') {
      return (
        <article key={block.id} className="shell-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{block.title}</p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{block.description}</p>
            </div>
            {renderBlockAction(block)}
          </div>
          <div className="mt-4 space-y-3">
            {blockItems(overview.alerts, block).length === 0 ? (
              <p className="text-sm leading-6 text-[var(--text-secondary)]">Nenhum alerta novo no momento.</p>
            ) : (
              blockItems(overview.alerts, block).map((alert) => (
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
        </article>
      );
    }

    if (block.blockType === 'team_context') {
      return (
        <article key={block.id} className="shell-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{block.title}</p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{block.description}</p>
            </div>
            {renderBlockAction(block, 'Ver equipe', '/users')}
          </div>
          <div className="mt-4 grid gap-3">
            {blockItems(overview.teamAndContext, block).map((item) => (
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
        </article>
      );
    }

    if (block.blockType === 'recent') {
      return (
        <article key={block.id} className="shell-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{block.title}</p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{block.description}</p>
            </div>
            {renderBlockAction(block, 'Abrir biblioteca', '/library')}
          </div>
          <div className="mt-5 grid gap-3">
            {blockItems(overview.recentItems, block).map((item) => (
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
        </article>
      );
    }

    if (block.blockType === 'quick_links') {
      return (
        <article key={block.id} className="shell-surface p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{block.title}</p>
              <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{block.description}</p>
            </div>
            {block.ctaLabel && block.ctaPath ? (
              renderBlockAction(block)
            ) : (
              <button type="button" className="btn-secondary justify-center" onClick={openSearch}>
                <FiSearch size={16} />
                Abrir busca
              </button>
            )}
          </div>
          <div className="mt-5 grid gap-3">
            {blockItems(quickLinks, block).map((item) => (
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
          </div>
        </article>
      );
    }

    return null;
  };

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
          Busque informacoes do workspace, abra uma nova tarefa e acompanhe o que pede atencao.
        </p>
      </section>

      <WorkspaceNotice
        title="Centro de comando"
        description="Comece por uma busca ou abra uma tarefa nova quando ja souber o trabalho que precisa ser feito."
        state="live"
        detail={overview?.supportingText}
      />

      <section className="manus-composer mx-auto w-full max-w-5xl p-5 sm:p-6">
        <form onSubmit={handleSubmit}>
          <div className="mb-5 flex flex-wrap gap-3">
            <button
              type="button"
              className="pill-button"
              style={mode === 'search' ? { borderColor: 'var(--surface-border-strong)', background: 'var(--fill-tsp-white-dark)' } : undefined}
              onClick={() => handleModeChange('search')}
            >
              Buscar
            </button>
            <button
              type="button"
              className="pill-button"
              style={mode === 'task' ? { borderColor: 'var(--surface-border-strong)', background: 'var(--fill-tsp-white-dark)' } : undefined}
              onClick={() => handleModeChange('task')}
            >
              Nova tarefa
            </button>
          </div>

          {mode === 'search' ? (
            <div className="rounded-[20px] border px-5 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              <input
                aria-label="Buscar no workspace"
                autoComplete="off"
                className="w-full border-none bg-transparent text-lg leading-8 outline-none placeholder:text-[var(--text-tertiary)]"
                placeholder="Busque por tarefa, projeto, pessoa, arquivo ou contexto."
                type="search"
                value={prompt}
                onChange={(event) => setPrompt(event.target.value)}
              />
            </div>
          ) : (
            <textarea
              aria-label="Nova tarefa"
              className="min-h-[220px] w-full resize-none border-none bg-transparent text-lg leading-8 outline-none placeholder:text-[var(--text-tertiary)]"
              placeholder="Descreva o trabalho que voce quer abrir."
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
            />
          )}

          <div className="mt-5 rounded-[16px] border px-4 py-3" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
            <div className="flex flex-wrap items-center gap-2">
              <button type="button" className="icon-button" aria-label="Adicionar contexto">
                <FiPlus size={16} />
              </button>

              <button type="submit" className="btn-primary ml-auto" disabled={submitting}>
                {submitting ? (mode === 'search' ? 'Buscando...' : 'Abrindo...') : (mode === 'search' ? 'Buscar agora' : 'Abrir tarefa')}
                <FiArrowRight size={16} />
              </button>
            </div>
          </div>

          {mode === 'task' ? (
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
          ) : null}

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

            {mode === 'task' ? (
              <p className="text-sm text-[var(--text-secondary)]">
                Tipo selecionado: <span className="font-semibold text-[var(--text-primary)]">{activeTaskType?.label ?? 'Pesquisar'}</span>
              </p>
            ) : (
              <p className="text-sm text-[var(--text-secondary)]">
                O resultado abre a area certa com base nos dados reais do workspace.
              </p>
            )}
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
            <section className="grid gap-5 xl:grid-cols-2">
              {orderedBlocks.map((block) => renderBlock(block.blockType))}
            </section>
          </>
        ) : null}
      </AsyncState>
    </div>
  );
}
