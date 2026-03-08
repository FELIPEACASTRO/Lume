import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Agents from './Agents';

const mockUseShell = vi.fn();
const mockFindProfiles = vi.fn();
const mockFindThreads = vi.fn();
const mockFindMessages = vi.fn();
const mockUpdateRuntime = vi.fn();
const mockFindProviders = vi.fn();
const mockFindModels = vi.fn();
const mockFindProviderStatuses = vi.fn();

vi.mock('../components/shell/ShellContext', () => ({
  useShell: () => mockUseShell(),
}));

vi.mock('../services/agentService', () => ({
  agentService: {
    findProfiles: () => mockFindProfiles(),
    findThreads: () => mockFindThreads(),
    findMessages: (...args: unknown[]) => mockFindMessages(...args),
    updateRuntime: (...args: unknown[]) => mockUpdateRuntime(...args),
    createThread: vi.fn(),
    appendMessage: vi.fn(),
  },
}));

vi.mock('../services/providerService', () => ({
  providerService: {
    findProviders: () => mockFindProviders(),
    findModels: () => mockFindModels(),
    findProviderStatuses: () => mockFindProviderStatuses(),
  },
}));

describe('Agents', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseShell.mockReturnValue({
      refreshSummary: vi.fn().mockResolvedValue(undefined),
      session: {
        role: {
          permissions: ['agents.runtime.manage'],
        },
      },
    });
    mockFindProfiles.mockResolvedValue([
      {
        id: 'ops',
        name: 'Ops Strategist',
        specialty: 'Operacao',
        description: 'Traduz pedidos em fluxo.',
        status: 'ok',
        availability: 'live',
        note: 'runtime',
        providerCode: 'openai',
        modelCode: 'openai:gpt-4.1-mini',
        versionLabel: 'agent-v1-openai',
        apiStyle: 'responses',
        credentialState: 'configured',
        catalogState: 'live',
        configured: true,
        executionSupported: true,
        toolset: ['chat'],
      },
    ]);
    mockFindThreads.mockResolvedValue([
      {
        id: 'thread-1',
        agentProfileId: 'ops',
        agentName: 'Ops Strategist',
        title: 'Thread',
        status: 'ok',
        availability: 'live',
        lastMessagePreview: 'preview',
        updatedAt: '2026-03-08 00:00',
        providerCode: 'openai',
        modelCode: 'openai:gpt-4.1-mini',
        versionLabel: 'agent-v1-openai',
        apiStyle: 'responses',
        credentialState: 'configured',
        catalogState: 'live',
      },
    ]);
    mockFindMessages.mockResolvedValue([]);
    mockFindProviders.mockResolvedValue([
      {
        code: 'openai',
        name: 'OpenAI',
        category: 'text-runtime',
        protocol: 'OPENAI_RESPONSES',
        apiStyle: 'responses',
        executionSupported: true,
        configured: true,
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
        notes: 'n/a',
      },
      {
        code: 'anthropic',
        name: 'Anthropic',
        category: 'text-runtime',
        protocol: 'ANTHROPIC_MESSAGES',
        apiStyle: 'messages',
        executionSupported: true,
        configured: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        businessPriority: 'high_roi',
        syncMode: 'sync',
        adminOnly: false,
        tenantScoped: false,
        supportsResponsesApi: false,
        supportsChatCompletions: false,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        catalogState: 'live',
        pricingSummary: 'Pago por token.',
        rateLimitSummary: 'Rate limit por tier.',
        routingModes: ['quality-first', 'cost-first'],
        documentationSource: 'primary_docs',
        requiredHeaders: ['x-api-key'],
        credentialFields: [],
        apiKeyPortalUrl: 'https://example.com',
        docsUrl: 'https://docs.example.com',
        defaultModelCode: 'anthropic:claude-sonnet-4-5',
        capabilities: ['chat'],
        notes: 'n/a',
      },
    ]);
    mockFindModels.mockResolvedValue([
      {
        code: 'openai:gpt-4.1-mini',
        providerCode: 'openai',
        label: 'GPT-4.1 mini',
        versionLabel: 'agent-v1-openai',
        apiStyle: 'responses',
        catalogState: 'live',
        defaultModel: true,
        enabledForAgents: true,
      },
      {
        code: 'anthropic:claude-sonnet-4-5',
        providerCode: 'anthropic',
        label: 'Claude Sonnet 4.5',
        versionLabel: 'agent-v1-claude',
        apiStyle: 'messages',
        catalogState: 'live',
        defaultModel: true,
        enabledForAgents: true,
      },
    ]);
    mockFindProviderStatuses.mockResolvedValue([
      {
        providerCode: 'openai',
        providerName: 'OpenAI',
        configured: true,
        executionSupported: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        catalogState: 'live',
        category: 'text-runtime',
        adminOnly: false,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        readinessStatus: 'ready',
        missingCredentialEnvVars: [],
      },
      {
        providerCode: 'anthropic',
        providerName: 'Anthropic',
        configured: true,
        executionSupported: true,
        implementationStatus: 'live',
        evidenceLevel: 'integration_verified',
        catalogState: 'live',
        category: 'text-runtime',
        adminOnly: false,
        streamingMode: 'unsupported',
        runtimeMaturity: 'live',
        readinessStatus: 'ready',
        missingCredentialEnvVars: [],
      },
    ]);
    mockUpdateRuntime.mockResolvedValue({
      id: 'ops',
      name: 'Ops Strategist',
      specialty: 'Operacao',
      description: 'Traduz pedidos em fluxo.',
      status: 'ok',
      availability: 'live',
      note: 'runtime',
      providerCode: 'anthropic',
      modelCode: 'anthropic:claude-sonnet-4-5',
      versionLabel: 'agent-v2-claude',
      apiStyle: 'messages',
      credentialState: 'configured',
      catalogState: 'live',
      configured: true,
      executionSupported: true,
      toolset: ['chat'],
    });
  });

  it('saves runtime updates with the selected provider, model and version', async () => {
    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Agents />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Runtime real por perfil')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByLabelText('Selecionar provider do agent')).toHaveValue('openai'));

    fireEvent.change(screen.getByLabelText('Selecionar provider do agent'), { target: { value: 'anthropic' } });
    await waitFor(() => expect(screen.getByLabelText('Selecionar provider do agent')).toHaveValue('anthropic'));
    fireEvent.change(screen.getByLabelText('Selecionar modelo do agent'), { target: { value: 'anthropic:claude-sonnet-4-5' } });
    await waitFor(() => expect(screen.getByLabelText('Selecionar modelo do agent')).toHaveValue('anthropic:claude-sonnet-4-5'));
    fireEvent.change(screen.getByLabelText('Versao do agent'), { target: { value: 'agent-v2-claude' } });
    fireEvent.click(screen.getByRole('button', { name: /Salvar runtime/i }));

    await waitFor(() => expect(mockUpdateRuntime).toHaveBeenCalledWith('ops', {
      providerCode: 'anthropic',
      modelCode: 'anthropic:claude-sonnet-4-5',
      versionLabel: 'agent-v2-claude',
    }));
  });
});
