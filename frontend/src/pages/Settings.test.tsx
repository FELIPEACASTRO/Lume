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
const mockGetShellCatalog = vi.fn();
const mockUpdateShellNavigationItem = vi.fn();
const mockUpdateShellTaskType = vi.fn();
const mockCreateShellNavigationItem = vi.fn();
const mockDeleteShellNavigationItem = vi.fn();
const mockCreateShellTaskType = vi.fn();
const mockDeleteShellTaskType = vi.fn();
const mockGetHomeCatalog = vi.fn();
const mockUpdateHomeSettings = vi.fn();
const mockCreateHomeBlock = vi.fn();
const mockUpdateHomeBlock = vi.fn();
const mockDeleteHomeBlock = vi.fn();

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

vi.mock('../services/shellCatalogService', () => ({
  shellCatalogService: {
    getCatalog: () => mockGetShellCatalog(),
    createNavigationItem: (...args: unknown[]) => mockCreateShellNavigationItem(...args),
    deleteNavigationItem: (...args: unknown[]) => mockDeleteShellNavigationItem(...args),
    updateNavigationItem: (...args: unknown[]) => mockUpdateShellNavigationItem(...args),
    createTaskType: (...args: unknown[]) => mockCreateShellTaskType(...args),
    deleteTaskType: (...args: unknown[]) => mockDeleteShellTaskType(...args),
    updateTaskType: (...args: unknown[]) => mockUpdateShellTaskType(...args),
  },
}));

vi.mock('../services/homeService', () => ({
  homeService: {
    getCatalog: () => mockGetHomeCatalog(),
    updateSettings: (...args: unknown[]) => mockUpdateHomeSettings(...args),
    createBlock: (...args: unknown[]) => mockCreateHomeBlock(...args),
    updateBlock: (...args: unknown[]) => mockUpdateHomeBlock(...args),
    deleteBlock: (...args: unknown[]) => mockDeleteHomeBlock(...args),
  },
}));

describe('Settings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['providers.read', 'providers.test', 'threat_intel.read', 'knowledge.read'],
        },
      },
      refreshSummary: vi.fn().mockResolvedValue(undefined),
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
          title: 'Providers',
          description: 'Catalogo',
          availability: 'live',
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
        statusLabel: 'Ativo',
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
        statusLabel: 'Ativo',
        availability: 'live',
        ownerName: 'Operacao',
        taskCount: 2,
        updatedAt: '09/03 10:10',
      },
    ]);
    mockGetShellCatalog.mockResolvedValue({
      items: [
        {
          id: 'home',
          label: 'Inicio',
          path: '/',
          description: 'Resumo do trabalho.',
          icon: 'home',
          availability: 'live',
          group: 'primary',
          sortOrder: 10,
          enabled: true,
          keywords: ['inicio'],
        },
      ],
      taskTypes: [
        {
          taskType: 'research',
          label: 'Pesquisar',
          description: 'Levantar contexto.',
          sortOrder: 10,
          enabled: true,
        },
      ],
    });
    mockGetHomeCatalog.mockResolvedValue({
      settings: {
        headline: 'O que voce quer fazer?',
        supportingText: 'Painel principal do workspace.',
      },
      blocks: [
        {
          id: 'in-progress',
          blockType: 'in_progress',
          title: 'Em andamento',
          description: 'Trabalho em curso.',
          sortOrder: 10,
          maxItems: 4,
          ctaLabel: 'Ver tarefas',
          ctaPath: '/tasks',
          enabled: true,
        },
      ],
    });
    mockUpdateShellNavigationItem.mockImplementation(async (id: string, request: Record<string, unknown>) => ({
      id,
      label: (request.label as string | undefined) ?? 'Inicio',
      path: (request.path as string | undefined) ?? '/',
      description: (request.description as string | undefined) ?? 'Resumo do trabalho.',
      icon: (request.icon as string | undefined) ?? 'home',
      availability: (request.availability as string | undefined) ?? 'live',
      group: (request.group as string | undefined) ?? 'primary',
      sortOrder: (request.sortOrder as number | undefined) ?? 10,
      enabled: (request.enabled as boolean | undefined) ?? true,
      keywords: (request.keywords as string[] | undefined) ?? ['inicio'],
    }));
    mockUpdateShellTaskType.mockImplementation(async (taskType: string, request: Record<string, unknown>) => ({
      taskType,
      label: (request.label as string | undefined) ?? 'Pesquisar',
      description: (request.description as string | undefined) ?? 'Levantar contexto.',
      sortOrder: (request.sortOrder as number | undefined) ?? 10,
      enabled: (request.enabled as boolean | undefined) ?? true,
    }));
    mockCreateShellNavigationItem.mockImplementation(async (request: Record<string, unknown>) => ({
      id: request.id,
      label: request.label,
      path: request.path,
      description: request.description,
      icon: request.icon,
      availability: request.availability,
      group: request.group,
      sortOrder: request.sortOrder,
      enabled: request.enabled ?? true,
      keywords: request.keywords ?? [],
    }));
    mockDeleteShellNavigationItem.mockResolvedValue(undefined);
    mockCreateShellTaskType.mockImplementation(async (request: Record<string, unknown>) => ({
      taskType: request.taskType,
      label: request.label,
      description: request.description,
      sortOrder: request.sortOrder,
      enabled: request.enabled ?? true,
    }));
    mockDeleteShellTaskType.mockResolvedValue(undefined);
    mockUpdateHomeSettings.mockImplementation(async (request: Record<string, unknown>) => ({
      headline: request.headline ?? 'O que voce quer fazer?',
      supportingText: request.supportingText ?? 'Painel principal do workspace.',
    }));
    mockCreateHomeBlock.mockImplementation(async (request: Record<string, unknown>) => ({
      id: request.id,
      blockType: request.blockType,
      title: request.title,
      description: request.description,
      sortOrder: request.sortOrder,
      maxItems: request.maxItems ?? 4,
      ctaLabel: request.ctaLabel ?? null,
      ctaPath: request.ctaPath ?? null,
      enabled: request.enabled ?? true,
    }));
    mockUpdateHomeBlock.mockImplementation(async (id: string, request: Record<string, unknown>) => ({
      id,
      blockType: 'in_progress',
      title: request.title ?? 'Em andamento',
      description: request.description ?? 'Trabalho em curso.',
      sortOrder: request.sortOrder ?? 10,
      maxItems: request.maxItems ?? 4,
      ctaLabel: request.ctaLabel ?? 'Ver tarefas',
      ctaPath: request.ctaPath ?? '/tasks',
      enabled: request.enabled ?? true,
    }));
    mockDeleteHomeBlock.mockResolvedValue(undefined);
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

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Providers' })).toBeInTheDocument());
    expect(screen.getByText('OpenAI')).toBeInTheDocument();
    expect(screen.getAllByText('Indisponivel').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Ativo').length).toBeGreaterThan(0);
    expect(screen.getByText(/Fonte: Estatico/i)).toBeInTheDocument();
    expect(screen.getByText(/Snapshot: Temporario/i)).toBeInTheDocument();
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
          title: 'Uso e budgets',
          description: 'Budgets do workspace',
          availability: 'live',
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

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Uso e budgets' })).toBeInTheDocument());
    await waitFor(() => expect(screen.getByDisplayValue('core_now')).toBeInTheDocument());
    expect(screen.getByLabelText('Limite de alerta do workspace')).toHaveValue(300);
    expect(screen.getByLabelText('Limite maximo do workspace')).toHaveValue(450);
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
    expect(screen.getByLabelText('Disponivel para tarefas')).toBeChecked();
  });

  it('renders and manages the home overview catalog for settings admins', async () => {
    const refreshSummary = vi.fn().mockResolvedValue(undefined);
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['settings.manage'],
        },
      },
      refreshSummary,
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 0,
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
          key: 'home-overview',
          title: 'Tela inicial',
          description: 'Ajuste a mensagem principal e os blocos exibidos no inicio.',
          availability: 'live',
        },
      ],
    });

    render(
      <MemoryRouter
        initialEntries={['/settings?section=home-overview']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Tela inicial' })).toBeInTheDocument());
    expect(screen.getByLabelText('Titulo principal')).toHaveValue('O que voce quer fazer?');
    expect(screen.getByLabelText('Texto de apoio')).toHaveValue('Painel principal do workspace.');

    fireEvent.change(screen.getByLabelText('Titulo principal'), { target: { value: 'Defina sua proxima acao' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar mensagem' }));

    await waitFor(() => expect(mockUpdateHomeSettings).toHaveBeenCalledWith(expect.objectContaining({
      headline: 'Defina sua proxima acao',
    })));

    fireEvent.change(screen.getByLabelText('ID do novo bloco'), { target: { value: 'recentes' } });
    fireEvent.change(screen.getByLabelText('Tipo do bloco'), { target: { value: 'recent' } });
    fireEvent.change(screen.getByLabelText('Titulo do novo bloco'), { target: { value: 'Recentes' } });
    fireEvent.change(screen.getByLabelText('Descricao do novo bloco'), { target: { value: 'Ultimas entregas e tarefas atualizadas.' } });
    fireEvent.change(screen.getByLabelText('Limite do novo bloco'), { target: { value: '5' } });
    fireEvent.change(screen.getByLabelText('Texto do CTA do novo bloco'), { target: { value: 'Abrir biblioteca' } });
    fireEvent.change(screen.getByLabelText('Rota do CTA do novo bloco'), { target: { value: '/library' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar bloco' }));

    await waitFor(() => expect(mockCreateHomeBlock).toHaveBeenCalledWith(expect.objectContaining({
      id: 'recentes',
      blockType: 'recent',
      maxItems: 5,
      ctaLabel: 'Abrir biblioteca',
      ctaPath: '/library',
    })));

    fireEvent.click(screen.getByRole('button', { name: 'Remover bloco recentes' }));
    await waitFor(() => expect(mockDeleteHomeBlock).toHaveBeenCalledWith('recentes'));
    expect(refreshSummary).toHaveBeenCalled();
  });

  it('renders workspace catalog section for settings admins', async () => {
    const refreshSummary = vi.fn().mockResolvedValue(undefined);
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['settings.manage'],
        },
      },
      refreshSummary,
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 0,
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
          key: 'workspace-catalog',
          title: 'Menu e tarefas',
          description: 'Ajustes sem deploy',
          availability: 'live',
        },
      ],
    });

    render(
      <MemoryRouter
        initialEntries={['/settings?section=workspace-catalog']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Menu e tarefas' })).toBeInTheDocument());
    expect(screen.getAllByLabelText('Nome')[0]).toHaveValue('Inicio');
    expect(screen.getAllByLabelText('Nome')[1]).toHaveValue('Pesquisar');
    expect(screen.getByRole('button', { name: 'Salvar area' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Salvar tipo' })).toBeInTheDocument();

    fireEvent.change(screen.getAllByLabelText('Rota')[0], { target: { value: '/painel' } });
    fireEvent.change(screen.getAllByLabelText('Icone')[0], { target: { value: 'search' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar area' }));

    await waitFor(() => expect(mockUpdateShellNavigationItem).toHaveBeenCalledWith(
      'home',
      expect.objectContaining({
        path: '/painel',
        icon: 'search',
      }),
    ));
    expect(refreshSummary).toHaveBeenCalled();
  });

  it('creates and removes shell catalog items and task types', async () => {
    const refreshSummary = vi.fn().mockResolvedValue(undefined);
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['settings.manage'],
        },
      },
      refreshSummary,
    });
    mockGetOverview.mockResolvedValue({
      organizationName: 'Lume',
      workspaceName: 'Workspace Principal',
      roleLabel: 'Workspace Admin',
      unreadNotifications: 1,
      knowledgeSources: 0,
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
          key: 'workspace-catalog',
          title: 'Menu e tarefas',
          description: 'Ajustes sem deploy',
          availability: 'live',
        },
      ],
    });

    render(
      <MemoryRouter initialEntries={['/settings?section=workspace-catalog']} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Settings />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Menu e tarefas' })).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('ID'), { target: { value: 'alerts' } });
    fireEvent.change(screen.getByLabelText('Nome da nova area'), { target: { value: 'Alertas' } });
    fireEvent.change(screen.getByLabelText('Rota da nova area'), { target: { value: '/alerts' } });
    fireEvent.change(screen.getByLabelText('Descricao da nova area'), { target: { value: 'Pendencias e alertas do workspace.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar area' }));

    await waitFor(() => expect(mockCreateShellNavigationItem).toHaveBeenCalledWith(expect.objectContaining({
      id: 'alerts',
      path: '/alerts',
    })));

    fireEvent.change(screen.getByLabelText('ID do tipo'), { target: { value: 'triage' } });
    fireEvent.change(screen.getByLabelText('Nome do novo tipo'), { target: { value: 'Triagem' } });
    fireEvent.change(screen.getByLabelText('Descricao do novo tipo'), { target: { value: 'Classificar urgencia e proximo passo.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar tipo' }));

    await waitFor(() => expect(mockCreateShellTaskType).toHaveBeenCalledWith(expect.objectContaining({
      taskType: 'triage',
      label: 'Triagem',
    })));

    const removeAreaButtons = screen.getAllByRole('button', { name: 'Remover area' });
    fireEvent.click(removeAreaButtons[1]);
    await waitFor(() => expect(mockDeleteShellNavigationItem).toHaveBeenCalledWith('alerts'));

    const removeTypeButtons = screen.getAllByRole('button', { name: 'Remover tipo' });
    fireEvent.click(removeTypeButtons[1]);
    await waitFor(() => expect(mockDeleteShellTaskType).toHaveBeenCalledWith('triage'));
  });
});
