import { render, screen, waitFor } from '@testing-library/react';
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

describe('Settings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['providers.read', 'providers.test', 'threat_intel.read'],
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
});
