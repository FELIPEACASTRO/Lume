import { useCallback, useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { toApiClientError } from '../../services/api';
import { sessionService } from '../../services/sessionService';
import { usageService } from '../../services/usageService';
import { workspaceService } from '../../services/workspaceService';
import { SessionContext, UsageSummaryDto, WorkspaceOptionDto, WorkspaceSummary } from '../../types';
import SearchModal from './SearchModal';
import { ShellProvider } from './ShellContext';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

export default function AppShell() {
  const { pathname } = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [session, setSession] = useState<SessionContext | null>(null);
  const [summary, setSummary] = useState<WorkspaceSummary | null>(null);
  const [usage, setUsage] = useState<UsageSummaryDto | null>(null);
  const [availableWorkspaces, setAvailableWorkspaces] = useState<WorkspaceOptionDto[]>([]);
  const [shellLoading, setShellLoading] = useState(true);
  const [shellError, setShellError] = useState<string | null>(null);
  const [switchingWorkspace, setSwitchingWorkspace] = useState(false);

  const loadShell = useCallback(async () => {
    try {
      setShellLoading(true);
      setShellError(null);
      const [sessionResponse, summaryResponse, usageResponse, workspacesResponse] = await Promise.all([
        sessionService.getSession(),
        workspaceService.getSummary(),
        usageService.getSummary(),
        workspaceService.getAvailableWorkspaces(),
      ]);
      setSession(sessionResponse);
      setSummary(summaryResponse);
      setUsage(usageResponse);
      setAvailableWorkspaces(workspacesResponse);
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

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
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
  }, []);

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

  return (
    <ShellProvider
      value={{
        openSearch: () => {
          setMobileOpen(false);
          setSearchOpen(true);
        },
        closeSearch: () => setSearchOpen(false),
        session,
        summary,
        usage,
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
