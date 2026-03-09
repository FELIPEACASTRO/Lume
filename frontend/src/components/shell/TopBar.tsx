import { ChangeEvent } from 'react';
import { FiBell, FiLogOut, FiMenu, FiSearch, FiSettings } from 'react-icons/fi';
import { Link, useLocation } from 'react-router-dom';
import { useShell } from './ShellContext';

interface TopBarProps {
  onMenuToggle: () => void;
  onSearchOpen: () => void;
  onLogout: () => void;
}

function resolveSectionTitle(pathname: string, navigation: ReturnType<typeof useShell>['navigation']) {
  const directMatch = navigation.find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`));
  if (directMatch) {
    return directMatch.label;
  }
  if (pathname.startsWith('/tasks')) {
    return 'Tarefas';
  }
  return 'Workspace';
}

export default function TopBar({ onMenuToggle, onSearchOpen, onLogout }: TopBarProps) {
  const { pathname } = useLocation();
  const {
    session,
    summary,
    usage,
    navigation,
    availableWorkspaces,
    switchWorkspace,
    switchingWorkspace,
  } = useShell();
  const unreadNotifications = usage?.unreadNotifications ?? 0;
  const usageLabel = usage ? `${usage.remainingCredits}/${usage.dailyCredits}` : '--/--';
  const activeWorkspaceId = session?.workspace.id ?? availableWorkspaces.find((workspace) => workspace.active)?.id ?? 0;
  const title = resolveSectionTitle(pathname, navigation);

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
            <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[var(--text-secondary)]">
              {summary?.organizationName ?? session?.organization.name ?? 'Workspace'}
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1">
              <h1 className="text-lg font-semibold text-[var(--text-primary)]">{title}</h1>
              <p className="hidden text-sm text-[var(--text-secondary)] xl:block">
                {summary?.workspaceName ?? session?.workspace.name ?? 'Workspace ativo'}
              </p>
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

          <button
            type="button"
            className="flex h-11 min-w-[44px] items-center justify-center rounded-full border px-3 text-sm font-semibold"
            style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
            aria-label="Encerrar sessao"
            onClick={onLogout}
            title={`Sair de ${session?.user.email ?? 'sessao atual'}`}
          >
            {session?.user.initials ?? <FiLogOut size={16} />}
          </button>
        </div>
      </div>
    </header>
  );
}
