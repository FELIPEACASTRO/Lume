import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Settings from './Settings';

const mockUseShell = vi.fn();
const mockUseTheme = vi.fn();
const mockGetOverview = vi.fn();
const mockFindProviders = vi.fn();
const mockFindProviderStatuses = vi.fn();
const mockFindProviderHealth = vi.fn();
const mockFindProviderCredentials = vi.fn();
const mockTestConnectivity = vi.fn();
const mockGetCurrentBudget = vi.fn();
const mockUpdateCurrentBudget = vi.fn();
const mockFindKnowledgeSources = vi.fn();
const mockCreateKnowledgeSource = vi.fn();
const mockUpdateKnowledgeSource = vi.fn();
const mockRemoveKnowledgeSource = vi.fn();
const mockFindProjects = vi.fn();

vi.mock('../components/shell/ShellContext', () => ({
  useShell: () => mockUseShell(),
}));

vi.mock('../components/theme/ThemeProvider', () => ({
  useTheme: () => mockUseTheme(),
}));

vi.mock('../services/settingsService', () => ({
  settingsService: {
    getOverview: () => mockGetOverview(),
  },
}));

vi.mock('../services/providerService', () => ({
  providerService: {
    findProviders: () => mockFindProviders(),
    findProviderStatuses: () => mockFindProviderStatuses(),
    findProviderHealth: () => mockFindProviderHealth(),
    findProviderCredentials: () => mockFindProviderCredentials(),
    testConnectivity: (...args: unknown[]) => mockTestConnectivity(...args),
  },
}));

vi.mock('../services/budgetService', () => ({
  budgetService: {
    getCurrent: () => mockGetCurrentBudget(),
    updateCurrent: (...args: unknown[]) => mockUpdateCurrentBudget(...args),
  },
}));

vi.mock('../services/knowledgeSourceService', () => ({
  knowledgeSourceService: {
    findAll: (...args: unknown[]) => mockFindKnowledgeSources(...args),
    create: (...args: unknown[]) => mockCreateKnowledgeSource(...args),
    update: (...args: unknown[]) => mockUpdateKnowledgeSource(...args),
    remove: (...args: unknown[]) => mockRemoveKnowledgeSource(...args),
  },
}));

vi.mock('../services/projectService', () => ({
  projectService: {
    findAll: () => mockFindProjects(),
  },
}));

describe('Settings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['providers.read', 'providers.test', 'threat_intel.read', 'knowledge.read'],
        },
      },
    });
    mockUseTheme.mockReturnValue({
      preferences: {
        appearance: 'light',
        languageCode: 'pt-BR',
        emailUpdates: true,
        productUpdates: true,
      },
      preferencesError: null,
      updatePreferences: vi.fn(),
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 2,
      usage: {
        dailyCredits: 300,
        consumedCredits: 10,
        remainingCredits: 290,
        activeTasks: 1,
        scheduledTasks: 0,
        unreadNotifications: 1,
        note: 'ok',
        budget: {
          costCenter: 'core_now',
          chargebackMode: 'showback',
          softLimitCredits: 300,
          hardLimitCredits: 450,
          consumedCredits: 10,
          remainingSoftCredits: 290,
          remainingHardCredits: 440,
          softLimitUtilizationPercent: 3,
          hardLimitUtilizationPercent: 2,
          softLimitReached: false,
          hardLimitReached: false,
          budgetStatus: 'healthy',
          note: 'Budget operacional.',
        },
      },
      preferences: {
        appearance: 'light',
        languageCode: 'pt-BR',
        emailUpdates: true,
        productUpdates: true,
      },
      sections: [
        {
          key: 'providers-runtime',
          title: 'Providers & Runtime',
          description: 'Catalogo',
          availability: 'live',
          previewState: 'live',
        },
      ],
    });
    mockFindProviders.mockResolvedValue([
      {
        code: 'openai',
        name: 'OpenAI',
        category: 'text-runtime',
        protocol: 'OPENAI_RESPONSES',
        apiStyle: 'responses',
        executionSupported: true,
        configured: false,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        businessPriority: 'high_roi',
        syncMode: 'sync',
        adminOnly: false,
        tenantScoped: false,
        supportsResponsesApi: true,
        supportsChatCompletions: true,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        catalogState: 'live',
        pricingSummary: 'Pago por token.',
        rateLimitSummary: 'Rate limit por tier.',
        routingModes: ['cost-first', 'latency-first', 'quality-first'],
        documentationSource: 'primary_docs',
        requiredHeaders: ['Authorization'],
        credentialFields: [],
        apiKeyPortalUrl: 'https://example.com',
        docsUrl: 'https://docs.example.com',
        defaultModelCode: 'openai:gpt-4.1-mini',
        capabilities: ['chat'],
        notes: 'Responses API como caminho principal.',
      },
    ]);
    mockFindProviderStatuses.mockResolvedValue([
      {
        providerCode: 'openai',
        providerName: 'OpenAI',
        configured: false,
        executionSupported: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        catalogState: 'live',
        category: 'text-runtime',
        adminOnly: false,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        readinessStatus: 'missing_credentials',
        missingCredentialEnvVars: ['OPENAI_API_KEY'],
      },
    ]);
    mockFindProviderHealth.mockResolvedValue([
      {
        providerCode: 'openai',
        providerName: 'OpenAI',
        category: 'text-runtime',
        configured: false,
        executionSupported: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        readinessStatus: 'missing_credentials',
        healthSource: 'static',
        snapshotPersistence: 'memory',
        message: 'Credenciais obrigatorias ainda nao estao presentes no ambiente.',
        lastConnectivityStatus: null,
        lastCheckedAt: null,
        missingCredentialEnvVars: ['OPENAI_API_KEY'],
      },
    ]);
    mockFindProviderCredentials.mockResolvedValue([
      {
        providerCode: 'openai',
        providerName: 'OpenAI',
        configured: false,
        executionSupported: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        businessPriority: 'high_roi',
        syncMode: 'sync',
        category: 'text-runtime',
        apiStyle: 'responses',
        adminOnly: false,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        catalogState: 'live',
        pricingSummary: 'Pago por token.',
        rateLimitSummary: 'Rate limit por tier.',
        missingCredentialEnvVars: ['OPENAI_API_KEY'],
        credentialFields: [],
        apiKeyPortalUrl: 'https://example.com',
        docsUrl: 'https://docs.example.com',
      },
    ]);
    mockGetCurrentBudget.mockResolvedValue({
      costCenter: 'core_now',
      chargebackMode: 'showback',
      softLimitCredits: 300,
      hardLimitCredits: 450,
      consumedCredits: 10,
      remainingSoftCredits: 290,
      remainingHardCredits: 440,
      softLimitUtilizationPercent: 3,
      hardLimitUtilizationPercent: 2,
      softLimitReached: false,
      hardLimitReached: false,
      budgetStatus: 'healthy',
      note: 'Budget operacional.',
    });
    mockFindKnowledgeSources.mockResolvedValue([
      {
        id: 'knowledge-playbooks',
        title: 'Playbooks operacionais',
        sourceType: 'library',
        sourceUri: 'lume://library/playbooks',
        projectId: 'proj-ops',
        projectName: 'Operacao do workspace',
        statusLabel: 'API real',
        availability: 'live',
        documentCount: 3,
        enabledForAgents: true,
        note: 'Base operacional do workspace.',
        lastIndexedAt: '09/03 10:10',
        updatedAt: '09/03 10:10',
      },
    ]);
    mockFindProjects.mockResolvedValue([
      {
        id: 'proj-ops',
        name: 'Operacao do workspace',
        summary: 'Operacao',
        statusLabel: 'API real',
        availability: 'live',
        ownerName: 'Operacao',
        taskCount: 2,
        updatedAt: '09/03 10:10',
      },
    ]);
  });

  it('renders provider runtime metadata and health provenance', async () => {
    render(
      <MemoryRouter
        initialEntries={['/settings?section=providers-runtime']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Providers & Runtime' })).toBeInTheDocument());
    expect(screen.getByText('OpenAI')).toBeInTheDocument();
    expect(screen.getByText('unsupported')).toBeInTheDocument();
    expect(screen.getAllByText('live').length).toBeGreaterThan(0);
    expect(screen.getByText(/Fonte: static/i)).toBeInTheDocument();
    expect(screen.getByText(/Snapshot: memory/i)).toBeInTheDocument();
  });

  it('renders finops section with workspace budget controls', async () => {
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['budgets.read', 'budgets.manage', 'knowledge.read'],
        },
      },
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 2,
      usage: {
        dailyCredits: 300,
        consumedCredits: 10,
        remainingCredits: 290,
        activeTasks: 1,
        scheduledTasks: 0,
        unreadNotifications: 1,
        note: 'ok',
        budget: {
          costCenter: 'core_now',
          chargebackMode: 'showback',
          softLimitCredits: 300,
          hardLimitCredits: 450,
          consumedCredits: 10,
          remainingSoftCredits: 290,
          remainingHardCredits: 440,
          softLimitUtilizationPercent: 3,
          hardLimitUtilizationPercent: 2,
          softLimitReached: false,
          hardLimitReached: false,
          budgetStatus: 'healthy',
          note: 'Budget operacional.',
        },
      },
      preferences: {
        appearance: 'light',
        languageCode: 'pt-BR',
        emailUpdates: true,
        productUpdates: true,
      },
      sections: [
        {
          key: 'finops',
          title: 'FinOps & Budgets',
          description: 'Budgets do workspace',
          availability: 'live',
          previewState: 'live',
        },
      ],
    });

    render(
      <MemoryRouter
        initialEntries={['/settings?section=finops']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'FinOps & Budgets' })).toBeInTheDocument());
    expect(screen.getByDisplayValue('core_now')).toBeInTheDocument();
    expect(screen.getByLabelText('Soft limit do workspace')).toHaveValue(300);
    expect(screen.getByLabelText('Hard limit do workspace')).toHaveValue(450);
  });

  it('renders knowledge section with persisted sources and editable form', async () => {
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['knowledge.read', 'knowledge.manage'],
        },
      },
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 1,
      usage: {
        dailyCredits: 300,
        consumedCredits: 10,
        remainingCredits: 290,
        activeTasks: 1,
        scheduledTasks: 0,
        unreadNotifications: 1,
        note: 'ok',
        budget: {
          costCenter: 'core_now',
          chargebackMode: 'showback',
          softLimitCredits: 300,
          hardLimitCredits: 450,
          consumedCredits: 10,
          remainingSoftCredits: 290,
          remainingHardCredits: 440,
          softLimitUtilizationPercent: 3,
          hardLimitUtilizationPercent: 2,
          softLimitReached: false,
          hardLimitReached: false,
          budgetStatus: 'healthy',
          note: 'Budget operacional.',
        },
      },
      preferences: {
        appearance: 'light',
        languageCode: 'pt-BR',
        emailUpdates: true,
        productUpdates: true,
      },
      sections: [
        {
          key: 'knowledge',
          title: 'Knowledge',
          description: 'Fontes do workspace',
          availability: 'live',
          previewState: 'live',
        },
      ],
    });

    render(
      <MemoryRouter
        initialEntries={['/settings?section=knowledge']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Knowledge' })).toBeInTheDocument());
    expect(screen.getByText('Playbooks operacionais')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /editar/i }));
    expect(screen.getByDisplayValue('Playbooks operacionais')).toBeInTheDocument();
    expect(screen.getByLabelText('Projeto da fonte de conhecimento')).toBeInTheDocument();
    expect(screen.getByLabelText('Disponivel para agents')).toBeChecked();
  });
});
