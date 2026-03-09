import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { toApiClientError } from '../../services/api';
import { navigationService } from '../../services/navigationService';
import { sessionService } from '../../services/sessionService';
import { setupService } from '../../services/setupService';
import { usageService } from '../../services/usageService';
import { workspaceService } from '../../services/workspaceService';
import {
  BootstrapSetupRequest,
  LoginRequest,
  SessionContext,
  SetupStatusDto,
  ShellNavItem,
  ShellTaskTypeDto,
  UsageSummaryDto,
  WorkspaceOptionDto,
  WorkspaceSummary,
} from '../../types';
import SearchModal from './SearchModal';
import { ShellProvider } from './ShellContext';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

function SetupScreen({
  setupStatus,
  error,
  submitting,
  onSubmit,
}: {
  setupStatus: SetupStatusDto | null;
  error: string | null;
  submitting: boolean;
  onSubmit: (payload: BootstrapSetupRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<BootstrapSetupRequest>({
    organizationName: '',
    workspaceName: '',
    adminName: '',
    adminEmail: '',
    password: '',
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit(form);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--app-bg)] px-4 py-10 text-[var(--text-primary)]">
      <section className="shell-surface w-full max-w-2xl p-8">
        <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[var(--text-secondary)]">Primeiro acesso</p>
        <h1 className="mt-4 text-4xl leading-tight" style={{ fontFamily: 'var(--font-display)' }}>
          Configure a empresa e comece a operar.
        </h1>
        <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
          Esse passo cria a primeira organizacao, o primeiro workspace e o administrador inicial.
        </p>
        {setupStatus ? (
          <p className="mt-3 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
            Organizacoes: {setupStatus.organizations} | Workspaces: {setupStatus.workspaces} | Usuarios: {setupStatus.users}
          </p>
        ) : null}

        <form className="mt-8 grid gap-4" onSubmit={(event) => void handleSubmit(event)}>
          <label className="grid gap-2">
            <span className="text-sm font-semibold">Organizacao</span>
            <input
              className="shell-input min-h-[48px]"
              value={form.organizationName}
              onChange={(event) => setForm((current) => ({ ...current, organizationName: event.target.value }))}
              required
            />
          </label>
          <label className="grid gap-2">
            <span className="text-sm font-semibold">Workspace</span>
            <input
              className="shell-input min-h-[48px]"
              value={form.workspaceName}
              onChange={(event) => setForm((current) => ({ ...current, workspaceName: event.target.value }))}
              required
            />
          </label>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-sm font-semibold">Nome do administrador</span>
              <input
                className="shell-input min-h-[48px]"
                value={form.adminName}
                onChange={(event) => setForm((current) => ({ ...current, adminName: event.target.value }))}
                required
              />
            </label>
            <label className="grid gap-2">
              <span className="text-sm font-semibold">Email do administrador</span>
              <input
                type="email"
                className="shell-input min-h-[48px]"
                value={form.adminEmail}
                onChange={(event) => setForm((current) => ({ ...current, adminEmail: event.target.value }))}
                required
              />
            </label>
          </div>
          <label className="grid gap-2">
            <span className="text-sm font-semibold">Senha inicial</span>
            <input
              type="password"
              className="shell-input min-h-[48px]"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              required
            />
          </label>
          {error ? <p className="text-sm font-medium text-[#df7d77]">{error}</p> : null}
          <button type="submit" className="btn-primary mt-2 justify-center" disabled={submitting}>
            {submitting ? 'Criando ambiente...' : 'Concluir configuracao'}
          </button>
        </form>
      </section>
    </div>
  );
}

function LoginScreen({
  error,
  submitting,
  onSubmit,
}: {
  error: string | null;
  submitting: boolean;
  onSubmit: (payload: LoginRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<LoginRequest>({
    email: '',
    password: '',
  });

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit(form);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--app-bg)] px-4 py-10 text-[var(--text-primary)]">
      <section className="shell-surface w-full max-w-lg p-8">
        <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-[var(--text-secondary)]">Acesso</p>
        <h1 className="mt-4 text-4xl leading-tight" style={{ fontFamily: 'var(--font-display)' }}>
          Entre para acessar seu workspace.
        </h1>
        <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
          Use suas credenciais para abrir as tarefas, projetos, arquivos e configuracoes do time.
        </p>
        <form className="mt-8 grid gap-4" onSubmit={(event) => void handleSubmit(event)}>
          <label className="grid gap-2">
            <span className="text-sm font-semibold">Email</span>
            <input
              type="email"
              className="shell-input min-h-[48px]"
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              required
            />
          </label>
          <label className="grid gap-2">
            <span className="text-sm font-semibold">Senha</span>
            <input
              type="password"
              className="shell-input min-h-[48px]"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              required
            />
          </label>
          {error ? <p className="text-sm font-medium text-[#df7d77]">{error}</p> : null}
          <button type="submit" className="btn-primary mt-2 justify-center" disabled={submitting}>
            {submitting ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </section>
    </div>
  );
}

export default function AppShell() {
  const { pathname } = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [setupStatus, setSetupStatus] = useState<SetupStatusDto | null>(null);
  const [accessState, setAccessState] = useState<'setup_required' | 'unauthenticated' | 'authenticated'>('unauthenticated');
  const [session, setSession] = useState<SessionContext | null>(null);
  const [summary, setSummary] = useState<WorkspaceSummary | null>(null);
  const [usage, setUsage] = useState<UsageSummaryDto | null>(null);
  const [navigation, setNavigation] = useState<ShellNavItem[]>([]);
  const [taskTypes, setTaskTypes] = useState<ShellTaskTypeDto[]>([]);
  const [availableWorkspaces, setAvailableWorkspaces] = useState<WorkspaceOptionDto[]>([]);
  const [shellLoading, setShellLoading] = useState(true);
  const [shellError, setShellError] = useState<string | null>(null);
  const [switchingWorkspace, setSwitchingWorkspace] = useState(false);
  const [submittingAccessForm, setSubmittingAccessForm] = useState(false);

  const loadShell = useCallback(async () => {
    try {
      setShellLoading(true);
      setShellError(null);

      const nextSetupStatus = await setupService.getStatus();
      setSetupStatus(nextSetupStatus);

      if (nextSetupStatus.setupRequired) {
        setAccessState('setup_required');
        setSession(null);
        setSummary(null);
        setUsage(null);
        setNavigation([]);
        setTaskTypes([]);
        setAvailableWorkspaces([]);
        return;
      }

      try {
        const sessionResponse = await sessionService.getSession();
        setAccessState('authenticated');
        setSession(sessionResponse);
      } catch (error) {
        const apiError = toApiClientError(error);
        if (apiError.status === 401) {
          setAccessState('unauthenticated');
          setSession(null);
          setSummary(null);
          setUsage(null);
          setNavigation([]);
          setTaskTypes([]);
          setAvailableWorkspaces([]);
          setShellError(null);
          return;
        }
        throw error;
      }

      const [summaryResponse, usageResponse, workspacesResponse, navigationResponse] = await Promise.all([
        workspaceService.getSummary(),
        usageService.getSummary(),
        workspaceService.getAvailableWorkspaces(),
        navigationService.getNavigation(),
      ]);
      setSummary(summaryResponse);
      setUsage(usageResponse);
      setAvailableWorkspaces(workspacesResponse);
      setNavigation(navigationResponse.items);
      setTaskTypes(navigationResponse.taskTypes);
    } catch (error) {
      setShellError(toApiClientError(error).message);
    } finally {
      setShellLoading(false);
    }
  }, []);

  const switchWorkspace = useCallback(async (workspaceId: number) => {
    try {
      setSwitchingWorkspace(true);
      setShellError(null);
      await workspaceService.activateWorkspace(workspaceId);
      await loadShell();
    } catch (error) {
      setShellError(toApiClientError(error).message);
    } finally {
      setSwitchingWorkspace(false);
    }
  }, [loadShell]);

  const handleBootstrap = useCallback(async (payload: BootstrapSetupRequest) => {
    try {
      setSubmittingAccessForm(true);
      setShellError(null);
      const nextSession = await setupService.bootstrap(payload);
      setSession(nextSession);
      setAccessState('authenticated');
      await loadShell();
    } catch (error) {
      setShellError(toApiClientError(error).message);
    } finally {
      setSubmittingAccessForm(false);
    }
  }, [loadShell]);

  const handleLogin = useCallback(async (payload: LoginRequest) => {
    try {
      setSubmittingAccessForm(true);
      setShellError(null);
      const nextSession = await sessionService.login(payload);
      setSession(nextSession);
      setAccessState('authenticated');
      await loadShell();
    } catch (error) {
      setShellError(toApiClientError(error).message);
    } finally {
      setSubmittingAccessForm(false);
    }
  }, [loadShell]);

  const handleLogout = useCallback(async () => {
    try {
      await sessionService.logout();
    } finally {
      setAccessState('unauthenticated');
      setSession(null);
      setSummary(null);
      setUsage(null);
      setNavigation([]);
      setTaskTypes([]);
      setAvailableWorkspaces([]);
      setSearchOpen(false);
      setMobileOpen(false);
    }
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && accessState === 'authenticated') {
        event.preventDefault();
        setSearchOpen((current) => !current);
      }

      if (event.key === 'Escape') {
        setMobileOpen(false);
        setSearchOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [accessState]);

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  useEffect(() => {
    void loadShell();
  }, [loadShell]);

  useEffect(() => {
    document.body.style.overflow = mobileOpen || searchOpen ? 'hidden' : '';

    return () => {
      document.body.style.overflow = '';
    };
  }, [mobileOpen, searchOpen]);

  if (shellLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--app-bg)] px-4 text-[var(--text-primary)]">
        <div className="shell-surface w-full max-w-lg p-8 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.24em] text-[var(--text-secondary)]">Carregando</p>
          <p className="mt-4 text-sm leading-7 text-[var(--text-secondary)]">
            Preparando seu acesso e carregando os dados do workspace.
          </p>
        </div>
      </div>
    );
  }

  if (accessState === 'setup_required') {
    return <SetupScreen setupStatus={setupStatus} error={shellError} submitting={submittingAccessForm} onSubmit={handleBootstrap} />;
  }

  if (accessState === 'unauthenticated') {
    return <LoginScreen error={shellError} submitting={submittingAccessForm} onSubmit={handleLogin} />;
  }

  return (
    <ShellProvider
      value={{
        openSearch: () => {
          setMobileOpen(false);
          setSearchOpen(true);
        },
        closeSearch: () => setSearchOpen(false),
        accessState,
        setupStatus,
        session,
        summary,
        usage,
        navigation,
        taskTypes,
        availableWorkspaces,
        shellLoading,
        shellError,
        switchingWorkspace,
        refreshSummary: loadShell,
        switchWorkspace,
      }}
    >
      <div className="min-h-screen bg-[var(--app-bg)] text-[var(--text-primary)]">
        <Sidebar
          mobileOpen={mobileOpen}
          onClose={() => setMobileOpen(false)}
          onOpenSearch={() => {
            setMobileOpen(false);
            setSearchOpen(true);
          }}
        />

        <div className="lg:pl-[320px]">
          <TopBar
            onLogout={() => void handleLogout()}
            onMenuToggle={() => setMobileOpen(true)}
            onSearchOpen={() => {
              setMobileOpen(false);
              setSearchOpen(true);
            }}
          />

          <main className="min-h-[calc(100vh-120px)] px-4 pb-12 pt-6 sm:px-6 lg:px-8 lg:pt-8">
            <Outlet />
          </main>
        </div>

        <SearchModal open={searchOpen} onClose={() => setSearchOpen(false)} />
      </div>
    </ShellProvider>
  );
}
