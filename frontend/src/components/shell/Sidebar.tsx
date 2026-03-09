import { useEffect, useRef } from 'react';
import { IconType } from 'react-icons';
import {
  FiBookOpen,
  FiChevronRight,
  FiCpu,
  FiFolder,
  FiHome,
  FiLayers,
  FiSearch,
  FiSettings,
  FiUsers,
  FiX,
  FiZap,
  FiBell,
  FiBarChart2,
} from 'react-icons/fi';
import { Link, NavLink } from 'react-router-dom';
import { ShellIconKey, WorkspaceGroup } from '../../types';
import StatusBadge from '../common/StatusBadge';
import { useShell } from './ShellContext';

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
  onOpenSearch: () => void;
}

const iconMap: Record<ShellIconKey, IconType> = {
  home: FiHome,
  users: FiUsers,
  agents: FiCpu,
  library: FiBookOpen,
  projects: FiLayers,
  tasks: FiFolder,
  search: FiSearch,
  inbox: FiBell,
  usage: FiBarChart2,
  settings: FiSettings,
};

const navigationSections: Array<{ key: WorkspaceGroup; title: string }> = [
  { key: 'primary', title: 'Workspace' },
  { key: 'secondary', title: 'Administracao' },
];

const focusableSelector = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

function SidebarContent({ onClose, onOpenSearch }: Omit<SidebarProps, 'mobileOpen'>) {
  const { session, summary, navigation } = useShell();
  const recentItems = summary?.recentItems ?? [];

  return (
    <div className="flex h-full flex-col gap-6 overflow-y-auto px-4 py-4">
      <div className="flex items-center justify-between rounded-[16px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
        <Link to="/" className="flex min-w-0 items-center gap-3" onClick={onClose}>
          <div className="flex h-11 w-11 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
            <img src="/lume.svg" alt="Lume" className="h-7 w-7" />
          </div>
          <div className="min-w-0">
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-secondary)]">Lume workspace</p>
            <p className="truncate text-base font-semibold text-[var(--text-primary)]">
              {summary?.workspaceName ?? session?.workspace.name ?? 'Workspace'}
            </p>
          </div>
        </Link>

        <button type="button" className="icon-button lg:hidden" aria-label="Fechar menu lateral" onClick={onClose}>
          <FiX size={18} />
        </button>
      </div>

      <div className="space-y-3">
        <Link to="/" className="btn-primary w-full justify-between" onClick={onClose}>
          <span>Nova tarefa</span>
          <FiZap size={16} />
        </Link>

        <button type="button" className="btn-secondary w-full justify-between" onClick={onOpenSearch}>
          <span className="flex items-center gap-2">
            <FiSearch size={16} />
            Pesquisar
          </span>
          <span className="rounded-full border px-2 py-1 text-[11px]" style={{ borderColor: 'var(--surface-border-main)' }}>
            Ctrl K
          </span>
        </button>
      </div>

      <div className="space-y-4">
        {navigationSections.map((section) => {
          const items = navigation.filter((item) => item.group === section.key);

          if (items.length === 0) {
            return null;
          }

          return (
            <div key={section.key} className="space-y-2">
              <p className="px-2 text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">
                {section.title}
              </p>
              <div className="space-y-2">
                {items.map((item) => {
                  const Icon = iconMap[item.icon];

                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      className={({ isActive }) => [
                        'group flex items-start gap-3 rounded-[12px] border px-4 py-3 transition-all duration-200',
                        isActive ? 'translate-x-[2px]' : 'hover:bg-[var(--fill-tsp-white-light)]',
                      ].join(' ')}
                      style={({ isActive }) => ({
                        borderColor: isActive ? 'var(--surface-border-strong)' : 'var(--surface-border-main)',
                        background: isActive ? 'var(--fill-tsp-white-dark)' : 'var(--fill-tsp-white-main)',
                      })}
                      onClick={onClose}
                    >
                      <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                        <Icon size={18} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm font-semibold text-[var(--text-primary)]">{item.label}</span>
                        </div>
                        <p className="mt-1 text-sm leading-5 text-[var(--text-secondary)]">{item.description}</p>
                      </div>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between px-2">
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">
            Historico de tarefas
          </p>
          <Link to="/tasks" className="text-xs font-semibold text-link" onClick={onClose}>
            Ver board
          </Link>
        </div>

        <div className="space-y-2">
          {recentItems.map((task) => (
            <Link
              key={task.id}
              to={task.path}
              className="block rounded-[12px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
              style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
              onClick={onClose}
            >
              <div className="flex flex-wrap items-center gap-2">
                <p className="text-sm font-semibold text-[var(--text-primary)]">{task.title}</p>
                <StatusBadge state={task.availability} />
              </div>
              <p className="mt-1 text-sm text-[var(--text-secondary)]">{task.summary}</p>
              <div className="mt-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
                {task.detail}
              </div>
            </Link>
          ))}

          {recentItems.length === 0 ? (
            <div className="rounded-[12px] border border-dashed px-4 py-6 text-sm text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
              Nenhuma tarefa recente ainda.
            </div>
          ) : null}
        </div>
      </div>

      <div className="mt-auto rounded-[16px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">
          Resumo do workspace
        </p>
        <p className="mt-3 text-sm font-semibold text-[var(--text-primary)]">
          {summary?.organizationName ?? session?.organization.name ?? 'Lume'} . {summary?.workspaceName ?? session?.workspace.name ?? 'Workspace'}
        </p>
        <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
          A shell, a busca e o resumo agora refletem apenas estado real do backend e do banco deste workspace.
        </p>
        <Link to="/settings" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-link" onClick={onClose}>
          Abrir settings
          <FiChevronRight size={16} />
        </Link>
      </div>
    </div>
  );
}

export default function Sidebar({ mobileOpen, onClose, onOpenSearch }: SidebarProps) {
  const drawerRef = useRef<HTMLElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!mobileOpen) {
      previousFocusRef.current?.focus();
      return;
    }

    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const timeoutId = window.setTimeout(() => {
      const firstFocusable = drawerRef.current?.querySelector<HTMLElement>(focusableSelector);
      firstFocusable?.focus();
    }, 10);

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') {
        return;
      }

      const focusableElements = drawerRef.current?.querySelectorAll<HTMLElement>(focusableSelector);

      if (!focusableElements || focusableElements.length === 0) {
        event.preventDefault();
        return;
      }

      const first = focusableElements[0];
      const last = focusableElements[focusableElements.length - 1];
      const activeElement = document.activeElement;

      if (event.shiftKey && activeElement === first) {
        event.preventDefault();
        last.focus();
      }

      if (!event.shiftKey && activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      window.clearTimeout(timeoutId);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [mobileOpen]);

  return (
    <>
      <aside
        className="fixed inset-y-0 left-0 z-30 hidden w-[320px] border-r lg:block"
        style={{ borderColor: 'var(--surface-border-main)', background: 'var(--shell-bg)' }}
      >
        <SidebarContent onClose={onClose} onOpenSearch={onOpenSearch} />
      </aside>

      {mobileOpen ? (
        <>
          <div
            aria-hidden="true"
            className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm transition-opacity duration-200 lg:hidden"
            onClick={onClose}
          />

          <aside
            ref={drawerRef}
            role="dialog"
            aria-modal="true"
            aria-label="Menu lateral do workspace"
            className="fixed inset-y-0 left-0 z-50 w-[320px] transition-transform duration-200 lg:hidden"
            style={{ background: 'var(--shell-bg)' }}
          >
            <SidebarContent onClose={onClose} onOpenSearch={onOpenSearch} />
          </aside>
        </>
      ) : null}
    </>
  );
}
