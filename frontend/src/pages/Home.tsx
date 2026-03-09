import { FormEvent, useEffect, useMemo, useState } from 'react';
import { FiArrowRight, FiLayers, FiPlus, FiSearch } from 'react-icons/fi';
import { Link, useNavigate } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { toApiClientError } from '../services/api';
import { taskService } from '../services/taskService';

export default function Home() {
  const navigate = useNavigate();
  const { openSearch, summary, shellLoading, shellError, refreshSummary, taskTypes, navigation } = useShell();
  const [prompt, setPrompt] = useState('');
  const [selectedTaskType, setSelectedTaskType] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const recentItems = summary?.recentItems ?? [];
  const workspaceFacets = summary?.workspaceFacets ?? [];

  useEffect(() => {
    if (!selectedTaskType && taskTypes.length > 0) {
      setSelectedTaskType(taskTypes[0].taskType);
    }
  }, [selectedTaskType, taskTypes]);

  const activeTaskType = useMemo(() => {
    return taskTypes.find((taskType) => taskType.taskType === selectedTaskType) ?? taskTypes[0] ?? null;
  }, [selectedTaskType, taskTypes]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextPrompt = prompt.trim();
    if (!nextPrompt) {
      setSubmitError('Descreva a tarefa antes de enviar.');
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
      await refreshSummary();
      navigate(`/tasks/${createdTask.task.id}`);
    } catch (error) {
      setSubmitError(toApiClientError(error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-frame flex flex-col gap-8 pb-10">
      <section className="mx-auto flex max-w-4xl flex-col items-center text-center">
        <span className="rounded-full border px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.24em]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-secondary)' }}>
          Lume workspace shell
        </span>
        <h1 className="mt-6 max-w-4xl text-4xl leading-tight text-[var(--text-primary)] sm:text-5xl lg:text-6xl" style={{ fontFamily: 'var(--font-display)' }}>
          Uma camada operacional de IA com tarefas, contexto e governanca reais.
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--text-secondary)] sm:text-lg">
          Tudo o que aparece aqui depende do backend e do banco do workspace atual.
        </p>
      </section>

      <WorkspaceNotice
        title="Shell dinamica e backend-first."
        description="Biblioteca, projetos, membros, inbox, uso, busca e tarefas sao carregados apenas a partir do estado real do workspace."
        state="live"
        detail={summary ? `${summary.counts.projects} projetos, ${summary.counts.tasks} tarefas, ${summary.counts.libraryEntries} itens e ${summary.counts.unreadNotifications} notificacoes agora.` : 'A shell esta aguardando o resumo real do workspace.'}
      />

      <section className="manus-composer mx-auto w-full max-w-5xl p-5 sm:p-6">
        <form onSubmit={handleSubmit}>
          <textarea
            aria-label="Prompt principal"
            className="min-h-[220px] w-full resize-none border-none bg-transparent text-lg leading-8 outline-none placeholder:text-[var(--text-tertiary)]"
            placeholder="Descreva a tarefa que o Lume deve registrar e executar a partir do workspace atual."
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
          />

          <div className="mt-5 rounded-[16px] border px-4 py-3" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
            <div className="flex flex-wrap items-center gap-2">
              <button type="button" className="icon-button" aria-label="Anexar arquivo">
                <FiPlus size={16} />
              </button>

              <button type="submit" className="btn-primary ml-auto" disabled={submitting}>
                {submitting ? 'Abrindo...' : 'Enviar'}
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
              Tipo ativo: <span className="font-semibold text-[var(--text-primary)]">{activeTaskType?.label ?? 'Livre'}</span>
            </p>
          </div>
        </form>

        {submitError ? <p className="mt-4 text-sm font-medium text-[#df7d77]">{submitError}</p> : null}
      </section>

      <AsyncState
        state={shellLoading ? 'loading' : shellError ? 'error' : recentItems.length === 0 ? 'empty' : 'live'}
        loadingLabel="Carregando resumo real do workspace..."
        errorTitle="O resumo do workspace nao respondeu."
        errorDescription="As APIs centrais da shell nao retornaram o estado esperado."
        errorDetail={shellError ?? undefined}
        onRetry={() => void refreshSummary()}
        emptyTitle="Ainda nao ha atividade suficiente neste workspace."
        emptyDescription="Crie uma nova tarefa, abra um projeto ou adicione conhecimento para iniciar a trilha operacional."
      >
        <section className="grid gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]">
          <div className="shell-surface p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Em movimento</p>
                <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">Tarefas recentes</h2>
              </div>
              <Link to="/tasks" className="btn-secondary px-4">
                Ver board
              </Link>
            </div>

            <div className="mt-5 space-y-3">
              {recentItems.map((task) => (
                <Link
                  key={task.id}
                  to={task.path}
                  className="block rounded-[12px] border px-5 py-5 transition-all duration-200 hover:-translate-y-0.5"
                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                >
                  <p className="text-lg font-semibold text-[var(--text-primary)]">{task.title}</p>
                  <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{task.summary}</p>
                  <p className="mt-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{task.detail}</p>
                </Link>
              ))}
            </div>
          </div>

          <div className="shell-surface p-6">
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Escopo atual</p>
            <h2 className="mt-4 text-3xl leading-tight text-[var(--text-primary)]" style={{ fontFamily: 'var(--font-display)' }}>
              Contexto real primeiro. Narrativa depois.
            </h2>
            <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
              Esta home resume apenas o que o backend realmente conhece sobre o workspace atual: tarefas, projetos, biblioteca e facetas operacionais.
            </p>

            <div className="mt-6 grid gap-3">
              {workspaceFacets.map((item) => (
                <div key={item.id} className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{item.label}</p>
                  <p className="mt-3 text-xl font-semibold text-[var(--text-primary)]">{item.headline}</p>
                  <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </AsyncState>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
        <div className="shell-surface p-6">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Areas</p>
            <h2 className="mt-2 text-2xl font-semibold text-[var(--text-primary)]">Mapa do produto atual</h2>
          </div>
          <div className="mt-5 grid gap-3">
            {navigation.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className="rounded-[12px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
              >
                <p className="text-base font-semibold text-[var(--text-primary)]">{item.label}</p>
                <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.description}</p>
              </Link>
            ))}
          </div>
        </div>

        <div className="grid gap-4">
          <div className="manus-banner">
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Task runtime</p>
            <h3 className="mt-4 text-2xl font-semibold text-[var(--text-primary)]">Tipos de tarefa servidos pelo backend</h3>
            <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
              O backend publica o catalogo operacional de tipos de tarefa e a home apenas consome esse contrato.
            </p>
          </div>

          <div className="manus-banner">
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Command center</p>
            <h3 className="mt-4 text-2xl font-semibold text-[var(--text-primary)]">Busca, shell e resumo seguem coerentes</h3>
            <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
              A camada visual so exibe entidades e secoes que existem de fato no backend do workspace.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
