import { ChangeEvent } from 'react';
import { FiBell, FiMenu, FiSearch, FiSettings } from 'react-icons/fi';
import { Link, useLocation } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';
import { WorkspaceDataState } from '../../types';
import { useShell } from './ShellContext';

interface TopBarProps {
  onMenuToggle: () => void;
  onSearchOpen: () => void;
}

function getRouteMeta(pathname: string) {
  if (pathname.startsWith('/users')) {
    return {
      eyebrow: 'Membros',
      title: 'Membros',
      subtitle: 'Memberships reais conectadas ao workspace ativo.',
      availability: 'live' as WorkspaceDataState,
      statusLabel: 'RBAC ativo',
    };
  }

  if (pathname.startsWith('/agents')) {
    return {
      eyebrow: 'Agents',
      title: 'Agents',
      subtitle: 'Threads reais com linguagem assistida e controles preview.',
      availability: 'preview' as WorkspaceDataState,
      statusLabel: 'Preview assistido',
    };
  }

  if (pathname.startsWith('/library')) {
    return {
      eyebrow: 'Biblioteca',
      title: 'Biblioteca',
      subtitle: 'Artefatos reais do workspace em grade densa.',
      availability: 'live' as WorkspaceDataState,
      statusLabel: 'Modulo real',
    };
  }

  if (pathname.startsWith('/projects')) {
    return {
      eyebrow: 'Projetos',
      title: 'Projetos',
      subtitle: 'Ownership, backlog e grupos de execucao.',
      availability: 'live' as WorkspaceDataState,
      statusLabel: 'Modulo real',
    };
  }

  if (pathname.startsWith('/tasks')) {
    return {
      eyebrow: 'Task view',
      title: 'Tarefas',
      subtitle: 'Execucao assistida com steps, contexto e proximos passos.',
      availability: 'preview' as WorkspaceDataState,
      statusLabel: 'Preview assistido',
    };
  }

  if (pathname.startsWith('/usage')) {
    return {
      eyebrow: 'Uso',
      title: 'Uso',
      subtitle: 'Creditos, metering e volume operacional.',
      availability: 'live' as WorkspaceDataState,
      statusLabel: 'Metering',
    };
  }

  if (pathname.startsWith('/inbox')) {
    return {
      eyebrow: 'Inbox',
      title: 'Inbox',
      subtitle: 'Eventos e alertas do workspace.',
      availability: 'live' as WorkspaceDataState,
      statusLabel: 'Modulo real',
    };
  }

  if (pathname.startsWith('/settings')) {
    return {
      eyebrow: 'Settings',
      title: 'Settings',
      subtitle: 'Preferencias reais e superficies preview honestas.',
      availability: 'preview' as WorkspaceDataState,
      statusLabel: 'Modal-page',
    };
  }

  return {
    eyebrow: 'Workspace',
    title: 'Lume OS',
    subtitle: 'Motor de tarefas e contexto com linguagem inspirada no Manus.',
    availability: 'live' as WorkspaceDataState,
    statusLabel: 'Shell ativa',
  };
}

export default function TopBar({ onMenuToggle, onSearchOpen }: TopBarProps) {
  const { pathname } = useLocation();
  const {
    session,
    summary,
    usage,
    availableWorkspaces,
    switchWorkspace,
    switchingWorkspace,
  } = useShell();
  const meta = getRouteMeta(pathname);
  const unreadNotifications = usage?.unreadNotifications ?? 0;
  const usageLabel = usage ? `${usage.remainingCredits}/${usage.dailyCredits}` : '--/--';
  const activeWorkspaceId = session?.workspace.id ?? availableWorkspaces.find((workspace) => workspace.active)?.id ?? 0;

  const handleWorkspaceChange = async (event: ChangeEvent<HTMLSelectElement>) => {
    await switchWorkspace(Number(event.target.value));
  };

  return (
    <header className="sticky top-0 z-20 border-b px-4 py-4 backdrop-blur-xl sm:px-6 lg:px-8" style={{ borderColor: 'var(--surface-border-light)', background: 'color-mix(in srgb, var(--app-bg) 82%, transparent)' }}>
      <div className="flex items-center justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <button type="button" className="icon-button lg:hidden" aria-label="Abrir menu lateral" onClick={onMenuToggle}>
            <FiMenu size={18} />
          </button>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-3">
              <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[var(--text-secondary)]">
                {meta.eyebrow}
              </p>
              <StatusBadge state={meta.availability} />
            </div>
            <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1">
              <h1 className="text-lg font-semibold text-[var(--text-primary)]">{meta.title}</h1>
              <p className="hidden text-sm text-[var(--text-secondary)] xl:block">{meta.subtitle}</p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button type="button" className="btn-secondary px-3" aria-label="Abrir busca global" onClick={onSearchOpen}>
            <FiSearch size={16} />
            <span className="hidden sm:inline">Buscar</span>
            <span className="hidden rounded-full border px-2 py-1 text-[11px] sm:inline-flex" style={{ borderColor: 'var(--surface-border-main)' }}>
              Ctrl K
            </span>
          </button>

          <div className="hidden rounded-full border px-3 py-2 text-sm font-medium md:flex md:items-center md:gap-2" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-secondary)' }}>
            <span className="text-[var(--accent)]">Credits</span>
            <span className="font-semibold text-[var(--text-primary)]">{usageLabel}</span>
          </div>

          <label className="hidden items-center gap-2 rounded-full border px-3 py-2 text-sm md:flex" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-secondary)' }}>
            <span>Workspace</span>
            <select
              className="min-w-[170px] border-none bg-transparent font-semibold text-[var(--text-primary)] outline-none"
              value={activeWorkspaceId}
              onChange={(event) => void handleWorkspaceChange(event)}
              disabled={switchingWorkspace || availableWorkspaces.length === 0}
              aria-label="Selecionar workspace ativo"
            >
              {availableWorkspaces.map((workspace) => (
                <option key={workspace.id} value={workspace.id}>
                  {workspace.name}
                </option>
              ))}
            </select>
          </label>

          <Link to="/inbox" className="icon-button relative" aria-label="Abrir inbox do workspace">
            <FiBell size={16} />
            {unreadNotifications > 0 ? (
              <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-[var(--accent)] px-1 text-[11px] font-semibold text-[var(--text-on-solid)]">
                {unreadNotifications}
              </span>
            ) : null}
          </Link>

          <Link to="/settings" className="icon-button" aria-label="Abrir configuracoes do workspace">
            <FiSettings size={16} />
          </Link>

          <Link
            to="/settings"
            className="flex h-11 min-w-[44px] items-center justify-center rounded-full border px-3 text-sm font-semibold"
            style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
            aria-label="Abrir perfil e configuracoes"
          >
            {session?.user.initials ?? 'LD'}
          </Link>
        </div>
      </div>

      <div className="mt-4 hidden items-center justify-between rounded-[12px] border px-4 py-3 lg:flex" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
        <p className="text-sm text-[var(--text-secondary)]">
          {summary?.organizationName ?? session?.organization.name ?? 'Lume'} . {summary?.workspaceName ?? session?.workspace.name ?? 'Workspace'}
        </p>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-[var(--text-secondary)]">Status da area</span>
          <span className="rounded-full border px-3 py-1 font-semibold" style={{ borderColor: 'var(--surface-border-main)', color: 'var(--text-primary)' }}>
            {meta.statusLabel}
          </span>
        </div>
      </div>
    </header>
  );
}
