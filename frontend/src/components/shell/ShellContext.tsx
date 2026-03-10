/* eslint-disable react-refresh/only-export-components */

import { ReactNode, createContext, useContext } from 'react';
import {
  SessionContext,
  SetupStatusDto,
  ShellNavItem,
  ShellTaskTypeDto,
  UsageSummaryDto,
  WorkspaceOptionDto,
  WorkspaceSummary,
} from '../../types';

export type ShellAccessState = 'setup_required' | 'unauthenticated' | 'authenticated';

interface ShellContextValue {
  openSearch: () => void;
  closeSearch: () => void;
  accessState: ShellAccessState;
  setupStatus: SetupStatusDto | null;
  session: SessionContext | null;
  summary: WorkspaceSummary | null;
  usage: UsageSummaryDto | null;
  navigation: ShellNavItem[];
  taskTypes: ShellTaskTypeDto[];
  availableWorkspaces: WorkspaceOptionDto[];
  shellLoading: boolean;
  shellError: string | null;
  switchingWorkspace: boolean;
  refreshSummary: () => Promise<void>;
  switchWorkspace: (workspaceId: number) => Promise<void>;
}

const ShellContext = createContext<ShellContextValue | null>(null);

interface ShellProviderProps {
  children: ReactNode;
  value: ShellContextValue;
}

export function ShellProvider({ children, value }: ShellProviderProps) {
  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>;
}

export function useShell() {
  const context = useContext(ShellContext);

  if (!context) {
    throw new Error('useShell must be used within ShellProvider');
  }

  return context;
}
