import { FormEvent, useMemo, useState } from 'react';
import {
  FiArrowRight,
  FiLayers,
  FiMic,
  FiPlus,
  FiSearch,
  FiSmile,
  FiSliders,
  FiZap,
} from 'react-icons/fi';
import { Link, useNavigate } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { quickActions, shellNavigation } from '../data/shell';
import { toApiClientError } from '../services/api';
import { taskService } from '../services/taskService';

const previewToolbarActions = [
  { id: 'models', label: 'Anthropic +10', icon: FiSliders },
  { id: 'connectors', label: 'Conectores', icon: FiZap },
  { id: 'emoji', label: 'Emoji', icon: FiSmile },
  { id: 'audio', label: 'Audio', icon: FiMic },
];

export default function Home() {
  const navigate = useNavigate();
  const { openSearch, summary, shellLoading, shellError, refreshSummary } = useShell();
  const [prompt, setPrompt] = useState('');
  const [selectedTaskType, setSelectedTaskType] = useState('slides');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const recentItems = summary?.recentItems ?? [];
  const workspaceFacets = summary?.workspaceFacets ?? [];

  const activeQuickAction = useMemo(() => {
    return quickActions.find((action) => action.taskType === selectedTaskType) ?? quickActions[0];
  }, [selectedTaskType]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextPrompt = prompt.trim() || activeQuickAction?.prompt || '';

    try {
      setSubmitting(true);
      setSubmitError(null);
      const createdTask = await taskService.create({
        prompt: nextPrompt,
        taskType: selectedTaskType,
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
          Lume inspirado no Manus
        </span>
        <h1 className="mt-6 max-w-4xl text-4xl leading-tight text-[var(--text-primary)] sm:text-5xl lg:text-6xl" style={{ fontFamily: 'var(--font-display)' }}>
          Um motor de tarefas com shell honesta, contexto real e visual de control room.
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--text-secondary)] sm:text-lg">
          O Lume continua operando com as features reais de hoje, mas agora em uma linguagem mais direta, escura e orientada a acao.
        </p>
      </section>

      <WorkspaceNotice
        title="Visual novo, promessa honesta."
        description="Biblioteca, projetos, membros, inbox, uso e busca continuam reais. Controles inspirados no Manus sem backend pronto aparecem como preview rotulado, nunca como feature fantasma."
        state="live"
        detail={summary ? `${summary.counts.projects} projetos, ${summary.counts.tasks} tarefas, ${summary.counts.libraryEntries} itens e ${summary.counts.unreadNotifications} notificacoes agora.` : 'A shell continua conectada aos contratos reais do workspace.'}
      />

      <section className="manus-composer mx-auto w-full max-w-5xl p-5 sm:p-6">
        <form onSubmit={handleSubmit}>
          <textarea
            aria-label="Prompt principal"
            className="min-h-[220px] w-full resize-none border-none bg-transparent text-lg leading-8 outline-none placeholder:text-[var(--text-tertiary)]"
            placeholder="Descreva a tarefa que o Lume deve abrir. O envio continua criando uma task persistida com steps visiveis."
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
          />

          <div className="mt-5 rounded-[16px] border px-4 py-3" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
            <div className="flex flex-wrap items-center gap-2">
              <button type="button" className="icon-button" aria-label="Anexar arquivo">
                <FiPlus size={16} />
              </button>

              {previewToolbarActions.map((action) => {
                const Icon = action.icon;

                return (
                  <button
                    key={action.id}
                    type="button"
                    className="pill-button"
                    aria-label={`${action.label} indisponivel nesta fase`}
                    title={`${action.label} aparece como preview nesta fase`}
                  >
                    <Icon size={16} />
                    {action.label}
                    <StatusBadge state="disabled-preview" />
                  </button>
                );
              })}

              <button type="submit" className="btn-primary ml-auto" disabled={submitting}>
                {submitting ? 'Abrindo...' : 'Enviar'}
                <FiArrowRight size={16} />
              </button>
            </div>
          </div>

          <div className="mt-5 flex flex-wrap gap-3">
            {quickActions.map((action) => (
              <button
                key={action.id}
                type="button"
                className="pill-button"
                style={selectedTaskType === action.taskType ? { borderColor: 'var(--surface-border-strong)', background: 'var(--fill-tsp-white-dark)' } : undefined}
                onClick={() => {
                  setSelectedTaskType(action.taskType);
                  setPrompt(action.prompt);
                }}
              >
                {action.title}
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
              Tipo ativo: <span className="font-semibold text-[var(--text-primary)]">{activeQuickAction?.title ?? 'Livre'}</span>
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
        emptyDescription="Crie uma tarefa, abra um projeto ou consulte a biblioteca para iniciar a trilha operacional."
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
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-lg font-semibold text-[var(--text-primary)]">{task.title}</p>
                    <StatusBadge state={task.availability} />
                  </div>
                  <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{task.summary}</p>
                  <p className="mt-4 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">{task.detail}</p>
                </Link>
              ))}
            </div>
          </div>

          <div className="shell-surface p-6">
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Escopo atual</p>
            <h2 className="mt-4 text-3xl leading-tight text-[var(--text-primary)]" style={{ fontFamily: 'var(--font-display)' }}>
              Workspaces reais primeiro. Ilusoes de produto depois.
            </h2>
            <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
              Esta home exibe o mesmo norte das outras telas: linguagem premium, mas sem esconder que parte do benchmark continua em preview.
            </p>

            <div className="mt-6 grid gap-3">
              {workspaceFacets.map((item) => (
                <div key={item.id} className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{item.label}</p>
                    <StatusBadge state={item.availability} />
                  </div>
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
            {shellNavigation.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className="rounded-[12px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
              >
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-base font-semibold text-[var(--text-primary)]">{item.label}</p>
                  <StatusBadge state={item.availability} />
                </div>
                <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{item.description}</p>
              </Link>
            ))}
          </div>
        </div>

        <div className="grid gap-4">
          <div className="manus-banner">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Preview honesto</p>
              <StatusBadge state="disabled-preview" />
            </div>
            <h3 className="mt-4 text-2xl font-semibold text-[var(--text-primary)]">Toolbar ampliada</h3>
            <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
              Model picker amplo, conectores, emoji e audio ja aparecem na shell, mas continuam desligados ate ganharem backend real.
            </p>
          </div>

          <div className="manus-banner">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Command center</p>
              <StatusBadge state="live" />
            </div>
            <h3 className="mt-4 text-2xl font-semibold text-[var(--text-primary)]">Busca e shell continuam reais</h3>
            <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
              A camada visual mudou, mas a espinha da aplicacao continua presa aos contratos reais de session, workspace, search, tasks, projects e members.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
