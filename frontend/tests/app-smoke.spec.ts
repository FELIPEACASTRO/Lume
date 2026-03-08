import { expect, test } from '@playwright/test';

const basePreferences = {
  appearance: 'light',
  languageCode: 'pt-BR',
  emailUpdates: true,
  productUpdates: true,
};

const baseSession = {
  user: { id: 1, name: 'Lume Operator', email: 'operator@lume.local', initials: 'LO' },
  organization: { id: 1, name: 'Lume', slug: 'lume' },
  workspace: { id: 1, name: 'Workspace Principal', slug: 'workspace-principal' },
  role: {
    code: 'workspace_admin',
    label: 'Workspace Admin',
    permissions: [
      'workspace.read',
      'workspace.switch',
      'members.manage',
      'providers.read',
      'providers.test',
      'agents.runtime.manage',
      'research.run',
      'threat_intel.read',
      'threat_intel.run',
      'threat_intel.manage',
    ],
  },
};

const baseWorkspaces = [
  {
    id: 1,
    name: 'Workspace Principal',
    slug: 'workspace-principal',
    organizationId: 1,
    organizationName: 'Lume',
    roleCode: 'workspace_admin',
    roleLabel: 'Workspace Admin',
    active: true,
  },
  {
    id: 2,
    name: 'Workspace Secundario',
    slug: 'workspace-secundario',
    organizationId: 1,
    organizationName: 'Lume',
    roleCode: 'workspace_admin',
    roleLabel: 'Workspace Admin',
    active: false,
  },
];

const baseUsage = {
  dailyCredits: 300,
  consumedCredits: 48,
  remainingCredits: 252,
  activeTasks: 2,
  scheduledTasks: 1,
  unreadNotifications: 1,
  note: 'Metadados operacionais de uso.',
};

const baseTasks = [
  {
    id: 'task-onboarding',
    projectId: 'proj-ops',
    projectName: 'Operacao do workspace',
    taskType: 'playbook',
    title: 'Estruturar onboarding operacional',
    prompt: 'Mapeie o fluxo de onboarding.',
    summary: 'Task assistida com steps e follow-up.',
    statusLabel: 'Preview assistido',
    availability: 'preview',
    ownerName: 'Operacao',
    updatedAt: '2026-03-08 00:00',
    scheduledFor: null,
    shareSlug: 'shared-task-onboarding',
  },
];

const baseLibraryEntries = [
  {
    id: 'lib-1',
    title: 'Playbook de onboarding',
    category: 'Playbook',
    status: 'API real',
    availability: 'live',
    owner: 'Operacao',
    sourceLabel: 'Backend do workspace',
    summary: 'Fluxo mestre para onboarding.',
    tags: ['onboarding', 'ops'],
  },
  {
    id: 'lib-2',
    title: 'Checklist de rollout',
    category: 'Checklist',
    status: 'API real',
    availability: 'live',
    owner: 'Produto',
    sourceLabel: 'Backend do workspace',
    summary: 'Checklist para rollout gradual.',
    tags: ['rollout', 'produto'],
  },
];

const baseProjects = [
  {
    id: 'proj-ops',
    name: 'Operacao do workspace',
    summary: 'Ownership e backlog real.',
    statusLabel: 'API real',
    availability: 'live',
    ownerName: 'Operacao',
    taskCount: 4,
    updatedAt: '2026-03-08 00:00',
  },
];

const baseMembers = [
  {
    id: 10,
    userId: 1,
    membershipId: 10,
    name: 'Lume Operator',
    email: 'operator@lume.local',
    active: true,
    roleCode: 'workspace_admin',
    roleLabel: 'Workspace Admin',
    currentUser: true,
    createdAt: '2026-03-08 00:00',
    updatedAt: '2026-03-08 00:00',
  },
];

const baseNotifications = [
  {
    id: 'notif-1',
    kind: 'task',
    title: 'Task pronta para revisao',
    body: 'A tarefa de onboarding ja tem plano inicial.',
    path: '/tasks/task-onboarding',
    read: false,
    createdAt: '2026-03-08 00:00',
  },
];

const baseProviders = [
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
    pricingSummary: 'Custos variam por token, modelo e tier.',
    rateLimitSummary: 'Rate limits variam por plano e modelo.',
    routingModes: ['cost-first', 'latency-first', 'quality-first'],
    documentationSource: 'primary_docs',
    requiredHeaders: ['Authorization: Bearer <OPENAI_API_KEY>'],
    credentialFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        envVar: 'OPENAI_API_KEY',
        required: true,
        secret: true,
        configured: false,
        description: 'Chave principal do projeto OpenAI.',
      },
    ],
    apiKeyPortalUrl: 'https://platform.openai.com/settings/organization/api-keys',
    docsUrl: 'https://developers.openai.com/api/docs/guides/text/',
    defaultModelCode: 'openai:gpt-4.1-mini',
    capabilities: ['chat', 'reasoning', 'multimodal'],
    notes: 'Responses API como caminho principal.',
  },
  {
    code: 'anthropic',
    name: 'Anthropic',
    category: 'text-runtime',
    protocol: 'ANTHROPIC_MESSAGES',
    apiStyle: 'messages',
    executionSupported: true,
    configured: false,
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
    pricingSummary: 'Custos variam por token, modelo e tier.',
    rateLimitSummary: 'Rate limits variam por plano e modelo.',
    routingModes: ['cost-first', 'quality-first'],
    documentationSource: 'primary_docs',
    requiredHeaders: ['x-api-key', 'anthropic-version: 2023-06-01'],
    credentialFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        envVar: 'ANTHROPIC_API_KEY',
        required: true,
        secret: true,
        configured: false,
        description: 'Chave x-api-key da Anthropic.',
      },
    ],
    apiKeyPortalUrl: 'https://console.anthropic.com/settings/keys',
    docsUrl: 'https://docs.anthropic.com/en/api/messages',
    defaultModelCode: 'anthropic:claude-sonnet-4-5',
    capabilities: ['chat', 'vision', 'reasoning'],
    notes: 'Messages API oficial.',
  },
  {
    code: 'exa',
    name: 'Exa',
    category: 'research-search',
    protocol: 'CUSTOM_RESEARCH',
    apiStyle: 'research',
    executionSupported: true,
    configured: false,
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
    pricingSummary: 'Custos variam por pesquisa e profundidade.',
    rateLimitSummary: 'Rate limits variam por chave e plano.',
    routingModes: ['cost-first', 'quality-first', 'latency-first'],
    documentationSource: 'primary_docs',
    requiredHeaders: ['x-api-key'],
    credentialFields: [
      {
        key: 'apiKey',
        label: 'API Key',
        envVar: 'EXA_API_KEY',
        required: true,
        secret: true,
        configured: false,
        description: 'Chave Exa.',
      },
    ],
    apiKeyPortalUrl: 'https://dashboard.exa.ai/api-keys',
    docsUrl: 'https://exa.ai/docs/reference/search',
    defaultModelCode: 'exa:search',
    capabilities: ['search', 'research'],
    notes: 'Pesquisa neural pronta para execucao real.',
  },
  {
    code: 'darkowl',
    name: 'DarkOwl',
    category: 'threat-intel',
    protocol: 'CUSTOM_THREAT_INTEL',
    apiStyle: 'threat-intel',
    executionSupported: false,
    configured: false,
    implementationStatus: 'blocked',
    evidenceLevel: 'offline_verified',
    businessPriority: 'contract_dependent',
    syncMode: 'manual',
    adminOnly: true,
    tenantScoped: true,
    supportsResponsesApi: false,
    supportsChatCompletions: false,
    streamingMode: 'unsupported',
    runtimeMaturity: 'catalog_only',
    catalogState: 'manual',
    pricingSummary: 'Custos e limites dependem de contrato e compliance.',
    rateLimitSummary: 'Rate limits variam por contrato e toda execucao exige auditoria.',
    routingModes: ['quality-first'],
    documentationSource: 'mixed_sources',
    requiredHeaders: ['X-DarkOwl-Date', 'X-DarkOwl-Authorization'],
    credentialFields: [
      {
        key: 'publicKey',
        label: 'Public Key',
        envVar: 'DARKOWL_PUBLIC_KEY',
        required: true,
        secret: true,
        configured: false,
        description: 'Chave publica DarkOwl.',
      },
      {
        key: 'privateKey',
        label: 'Private Key',
        envVar: 'DARKOWL_PRIVATE_KEY',
        required: true,
        secret: true,
        configured: false,
        description: 'Chave privada DarkOwl.',
      },
    ],
    apiKeyPortalUrl: 'https://www.darkowl.com',
    docsUrl: 'https://www.darkowl.com/wp-content/uploads/2022/02/API-Welcome-Packet.pdf',
    defaultModelCode: 'darkowl:search',
    capabilities: ['dark-web-search', 'threat-intel'],
    notes: 'Requer chave publica, privada e assinatura HMAC.',
  },
];

const baseProviderStatuses = [
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
  {
    providerCode: 'anthropic',
    providerName: 'Anthropic',
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
    missingCredentialEnvVars: ['ANTHROPIC_API_KEY'],
  },
  {
    providerCode: 'exa',
    providerName: 'Exa',
    configured: false,
    executionSupported: true,
    implementationStatus: 'live',
    evidenceLevel: 'integration_verified',
    catalogState: 'live',
    category: 'research-search',
    adminOnly: false,
    streamingMode: 'unsupported',
    runtimeMaturity: 'live',
    readinessStatus: 'missing_credentials',
    missingCredentialEnvVars: ['EXA_API_KEY'],
  },
  {
    providerCode: 'darkowl',
    providerName: 'DarkOwl',
    configured: false,
    executionSupported: false,
    implementationStatus: 'blocked',
    evidenceLevel: 'offline_verified',
    catalogState: 'manual',
    category: 'threat-intel',
    adminOnly: true,
    streamingMode: 'unsupported',
    runtimeMaturity: 'catalog_only',
    readinessStatus: 'manual',
    missingCredentialEnvVars: ['DARKOWL_PUBLIC_KEY', 'DARKOWL_PRIVATE_KEY'],
  },
];

const baseProviderCredentials = baseProviders.map((provider) => ({
  providerCode: provider.code,
  providerName: provider.name,
  configured: provider.configured,
  executionSupported: provider.executionSupported,
  implementationStatus: provider.implementationStatus,
  evidenceLevel: provider.evidenceLevel,
  businessPriority: provider.businessPriority,
  syncMode: provider.syncMode,
  category: provider.category,
  apiStyle: provider.apiStyle,
  adminOnly: provider.adminOnly,
  streamingMode: provider.streamingMode,
  runtimeMaturity: provider.runtimeMaturity,
  catalogState: provider.catalogState,
  pricingSummary: provider.pricingSummary,
  rateLimitSummary: provider.rateLimitSummary,
  missingCredentialEnvVars: provider.credentialFields.filter((field) => field.required && !field.configured).map((field) => field.envVar),
  credentialFields: provider.credentialFields,
  apiKeyPortalUrl: provider.apiKeyPortalUrl,
  docsUrl: provider.docsUrl,
}));

const baseProviderHealth = baseProviderStatuses.map((status) => ({
  providerCode: status.providerCode,
  providerName: status.providerName,
  category: status.category,
  configured: status.configured,
  executionSupported: status.executionSupported,
  implementationStatus: status.implementationStatus,
  evidenceLevel: status.evidenceLevel,
  streamingMode: status.streamingMode,
  runtimeMaturity: status.runtimeMaturity,
  readinessStatus: status.readinessStatus,
  healthSource: 'static',
  snapshotPersistence: 'memory',
  message: status.configured ? 'Provider configurado e pronto para execucao.' : 'Credenciais obrigatorias ainda nao estao presentes no ambiente.',
  lastConnectivityStatus: null,
  lastCheckedAt: null,
  missingCredentialEnvVars: status.missingCredentialEnvVars,
}));

const baseModels = [
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
];

const baseAgentProfiles = [
  {
    id: 'ops',
    name: 'Ops Strategist',
    specialty: 'Operacao e processos',
    description: 'Traduz pedidos em fluxos executaveis com foco em custo, risco e velocidade.',
    status: 'Configure credenciais',
    availability: 'disabled-preview',
    note: 'Configure OPENAI_API_KEY para ativar este runtime.',
    providerCode: 'openai',
    modelCode: 'openai:gpt-4.1-mini',
    versionLabel: 'agent-v1-openai',
    apiStyle: 'responses',
    credentialState: 'missing_credentials',
    catalogState: 'live',
    configured: false,
    executionSupported: true,
    toolset: ['chat', 'reasoning', 'multimodal'],
  },
];

const baseAgentThreads = [
  {
    id: 'thread-ops',
    agentProfileId: 'ops',
    agentName: 'Ops Strategist',
    title: 'Fluxo de onboarding operacional',
    status: 'Preview assistido',
    availability: 'preview',
    lastMessagePreview: 'Mapeie um fluxo de onboarding com checkpoints claros.',
    updatedAt: '08/03/2026 00:00',
    providerCode: 'openai',
    modelCode: 'openai:gpt-4.1-mini',
    versionLabel: 'agent-v1-openai',
    apiStyle: 'responses',
    credentialState: 'missing_credentials',
    catalogState: 'live',
  },
];

const baseAgentMessages = {
  'thread-ops': [
    {
      id: 'msg-user',
      role: 'user',
      body: 'Mapeie um fluxo de onboarding com checkpoints claros.',
      timestamp: '08/03/2026 00:00',
    },
    {
      id: 'msg-assistant',
      role: 'assistant',
      body: 'Runtime em preview: configure as credenciais para ativar a inferencia real.',
      timestamp: '08/03/2026 00:01',
    },
  ],
};

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function roleLabelFor(roleCode: string) {
  return roleCode === 'workspace_admin' ? 'Workspace Admin' : 'Workspace Member';
}

function buildSummary(workspaceName: string, userCount: number) {
  return {
    workspaceName,
    organizationName: 'Lume',
    counts: {
      users: userCount,
      libraryEntries: 2,
      agentThreads: 1,
      projects: 2,
      tasks: 2,
      unreadNotifications: 1,
    },
    recentItems: [
      {
        id: 'task-onboarding',
        title: 'Estruturar onboarding operacional',
        summary: 'Task assistida com steps e contexto.',
        detail: 'Atualizada agora',
        path: '/tasks/task-onboarding',
        availability: 'preview',
      },
    ],
    workspaceFacets: [
      {
        id: 'facet-library',
        label: 'Biblioteca',
        headline: 'Contexto real em API',
        description: 'Artefatos persistidos no backend.',
        availability: 'live',
        path: '/library',
      },
    ],
  };
}

function buildTaskDetail(task: (typeof baseTasks)[number]) {
  return {
    task,
    steps: [
      {
        id: 'step-1',
        stepOrder: 1,
        stepType: 'plan',
        title: 'Definir escopo',
        detail: 'Listar etapas e aprovacoes.',
        statusLabel: 'completed',
      },
      {
        id: 'step-2',
        stepOrder: 2,
        stepType: 'context',
        title: 'Cruzar biblioteca',
        detail: 'Buscar contexto no workspace.',
        statusLabel: 'running',
      },
    ],
    followUpSuggestions: ['Resuma o fluxo em bullets.', 'Prepare uma trilha de validacao.'],
  };
}

function buildSettingsOverview(preferences: typeof basePreferences, workspaceName: string) {
  return {
    organizationName: 'Lume',
    workspaceName,
    roleLabel: 'Workspace Admin',
    unreadNotifications: 1,
    knowledgeSources: 2,
    usage: baseUsage,
    preferences,
    sections: [
      {
        key: 'configuracoes',
        title: 'Configuracoes',
        description: 'Aparencia e comunicacao.',
        availability: 'live',
        previewState: 'live',
      },
      {
        key: 'mail',
        title: 'Mail',
        description: 'Preview de mail.',
        availability: 'preview',
        previewState: 'disabled-preview',
      },
      {
        key: 'providers-runtime',
        title: 'Providers & Runtime',
        description: 'Catalogo e runtime real dos agentes.',
        availability: 'live',
        previewState: 'live',
      },
      {
        key: 'threat-intelligence',
        title: 'Threat Intelligence',
        description: 'Secao administrativa e auditada.',
        availability: 'preview',
        previewState: 'disabled-preview',
      },
    ],
  };
}

test.beforeEach(async ({ page }) => {
  let preferences = deepClone(basePreferences);
  let sessionData = deepClone(baseSession);
  let workspacesData = deepClone(baseWorkspaces);
  const usageData = deepClone(baseUsage);
  let tasksData = deepClone(baseTasks);
  const libraryEntriesData = deepClone(baseLibraryEntries);
  const projectsData = deepClone(baseProjects);
  let membersData = deepClone(baseMembers);
  const providersData = deepClone(baseProviders);
  const providerHealthData = deepClone(baseProviderHealth);
  const providerStatusesData = deepClone(baseProviderStatuses);
  const providerCredentialsData = deepClone(baseProviderCredentials);
  const modelsData = deepClone(baseModels);
  let agentProfilesData = deepClone(baseAgentProfiles);
  let agentThreadsData = deepClone(baseAgentThreads);
  const agentMessagesData = deepClone(baseAgentMessages);
  const notificationsData = deepClone(baseNotifications);
  let summaryData = buildSummary(sessionData.workspace.name, membersData.length);
  let taskDetailsData = Object.fromEntries(tasksData.map((task) => [task.id, buildTaskDetail(task)]));
  let memberIdCounter = 20;

  const activateWorkspace = (workspaceId: number) => {
    const nextWorkspace = workspacesData.find((workspace) => workspace.id === workspaceId) ?? workspacesData[0];

    workspacesData = workspacesData.map((workspace) => ({
      ...workspace,
      active: workspace.id === nextWorkspace.id,
    }));

    sessionData = {
      ...sessionData,
      workspace: {
        id: nextWorkspace.id,
        name: nextWorkspace.name,
        slug: nextWorkspace.slug,
      },
    };

    summaryData = buildSummary(nextWorkspace.name, membersData.length);
  };

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname.endsWith('/api/v1/auth/session')) {
      return route.fulfill({ json: sessionData });
    }

    if (pathname.endsWith('/api/v1/workspaces')) {
      return route.fulfill({ json: workspacesData });
    }

    if (pathname.endsWith('/api/workspace/summary')) {
      return route.fulfill({ json: summaryData });
    }

    if (pathname.endsWith('/api/usage/summary')) {
      return route.fulfill({ json: usageData });
    }

    if (pathname.endsWith('/api/v1/settings/preferences') && request.method() === 'GET') {
      return route.fulfill({ json: preferences });
    }

    if (pathname.endsWith('/api/v1/settings/preferences') && request.method() === 'PATCH') {
      const payload = request.postDataJSON() as Partial<typeof preferences>;
      preferences = { ...preferences, ...payload };
      return route.fulfill({ json: preferences });
    }

    if (pathname.endsWith('/api/settings/overview')) {
      return route.fulfill({
        json: buildSettingsOverview(preferences, sessionData.workspace.name),
      });
    }

    if (pathname.endsWith('/api/v1/providers') && request.method() === 'GET') {
      return route.fulfill({ json: providersData });
    }

    if (pathname.endsWith('/api/v1/providers/health') && request.method() === 'GET') {
      return route.fulfill({ json: providerHealthData });
    }

    if (pathname.endsWith('/api/v1/providers/status') && request.method() === 'GET') {
      return route.fulfill({ json: providerStatusesData });
    }

    if (pathname.endsWith('/api/v1/provider-credentials') && request.method() === 'GET') {
      return route.fulfill({ json: providerCredentialsData });
    }

    if (pathname.endsWith('/api/v1/models') && request.method() === 'GET') {
      const providerCode = new URL(request.url()).searchParams.get('provider');
      const filteredModels = providerCode ? modelsData.filter((model) => model.providerCode === providerCode) : modelsData;
      return route.fulfill({ json: filteredModels });
    }

    if (pathname.includes('/api/v1/providers/') && pathname.endsWith('/connectivity-test') && request.method() === 'POST') {
      const segments = pathname.split('/');
      const providerCode = segments[segments.length - 2];
      const provider = providersData.find((item) => item.code === providerCode) ?? providersData[0];
      const status = providerStatusesData.find((item) => item.providerCode === providerCode);
      return route.fulfill({
        json: {
          providerCode,
          providerName: provider.name,
          category: provider.category,
          apiStyle: provider.apiStyle,
          status: status?.configured ? 'completed' : 'missing_credentials',
          configured: status?.configured ?? false,
          executionSupported: status?.executionSupported ?? provider.executionSupported,
          implementationStatus: status?.implementationStatus ?? provider.implementationStatus,
          evidenceLevel: status?.evidenceLevel ?? provider.evidenceLevel,
          streamingMode: status?.streamingMode ?? provider.streamingMode,
          runtimeMaturity: status?.runtimeMaturity ?? provider.runtimeMaturity,
          latencyMs: status?.configured ? 180 : null,
          message: status?.configured
            ? 'Connectivity test concluido.'
            : `Credenciais ausentes para ${provider.name}.`,
          missingCredentialEnvVars: status?.missingCredentialEnvVars ?? [],
        },
      });
    }

    if (pathname.endsWith('/api/tasks') && request.method() === 'GET') {
      return route.fulfill({ json: tasksData });
    }

    if (pathname.endsWith('/api/tasks') && request.method() === 'POST') {
      const payload = request.postDataJSON() as { prompt?: string; taskType?: string };
      const createdTask = {
        ...tasksData[0],
        id: 'task-generated',
        taskType: payload.taskType ?? 'slides',
        title: 'Nova task gerada a partir da home',
        prompt: payload.prompt ?? 'Nova tarefa criada.',
        summary: 'Task criada via composer principal.',
        updatedAt: '2026-03-08 00:30',
        shareSlug: null,
      };

      tasksData = [createdTask, ...tasksData.filter((task) => task.id !== createdTask.id)];
      taskDetailsData = { ...taskDetailsData, [createdTask.id]: buildTaskDetail(createdTask) };
      summaryData = {
        ...summaryData,
        recentItems: [
          {
            id: createdTask.id,
            title: createdTask.title,
            summary: createdTask.summary,
            detail: 'Criada agora',
            path: `/tasks/${createdTask.id}`,
            availability: createdTask.availability,
          },
          ...summaryData.recentItems,
        ],
      };

      return route.fulfill({ json: taskDetailsData[createdTask.id] });
    }

    if (pathname.startsWith('/api/tasks/')) {
      const taskId = pathname.split('/').pop() ?? '';
      return route.fulfill({ json: taskDetailsData[taskId] ?? buildTaskDetail(tasksData[0]) });
    }

    if (pathname.endsWith('/api/library/entries')) {
      return route.fulfill({ json: libraryEntriesData });
    }

    if (pathname.endsWith('/api/projects')) {
      return route.fulfill({ json: projectsData });
    }

    if (pathname.endsWith('/api/v1/members') && request.method() === 'GET') {
      return route.fulfill({ json: membersData });
    }

    if (pathname.endsWith('/api/v1/members') && request.method() === 'POST') {
      const payload = request.postDataJSON() as { name: string; email: string; roleCode: string };
      const createdMember = {
        id: memberIdCounter,
        userId: memberIdCounter,
        membershipId: memberIdCounter,
        name: payload.name,
        email: payload.email,
        active: true,
        roleCode: payload.roleCode,
        roleLabel: roleLabelFor(payload.roleCode),
        currentUser: false,
        createdAt: '2026-03-08 00:20',
        updatedAt: '2026-03-08 00:20',
      };

      memberIdCounter += 1;
      membersData = [...membersData, createdMember];
      summaryData = buildSummary(sessionData.workspace.name, membersData.length);
      return route.fulfill({ json: createdMember });
    }

    if (pathname.includes('/api/v1/members/') && request.method() === 'PATCH') {
      const membershipId = Number(pathname.split('/').pop());
      const payload = request.postDataJSON() as {
        active?: boolean;
        email?: string;
        name?: string;
        roleCode?: string;
      };

      membersData = membersData.map((member) => {
        if (member.membershipId !== membershipId) {
          return member;
        }

        const roleCode = payload.roleCode ?? member.roleCode;

        return {
          ...member,
          name: payload.name ?? member.name,
          email: payload.email ?? member.email,
          roleCode,
          roleLabel: roleLabelFor(roleCode),
          active: payload.active ?? member.active,
          updatedAt: '2026-03-08 00:40',
        };
      });

      summaryData = buildSummary(sessionData.workspace.name, membersData.length);
      const updatedMember = membersData.find((member) => member.membershipId === membershipId) ?? membersData[0];
      return route.fulfill({ json: updatedMember });
    }

    if (pathname.endsWith('/api/notifications')) {
      return route.fulfill({ json: notificationsData });
    }

    if (pathname.endsWith('/agents/profiles') && request.method() === 'GET') {
      return route.fulfill({ json: agentProfilesData });
    }

    if (pathname.endsWith('/agents/threads') && request.method() === 'GET') {
      return route.fulfill({ json: agentThreadsData });
    }

    if (pathname.endsWith('/agents/threads') && request.method() === 'POST') {
      return route.fulfill({
        json: {
          thread: agentThreadsData[0],
          messages: agentMessagesData['thread-ops'],
        },
      });
    }

    if (pathname.includes('/agents/threads/') && pathname.endsWith('/messages') && request.method() === 'GET') {
      const segments = pathname.split('/');
      const threadId = segments[segments.length - 2];
      return route.fulfill({ json: agentMessagesData[threadId] ?? agentMessagesData['thread-ops'] });
    }

    if (pathname.includes('/agents/threads/') && pathname.endsWith('/messages') && request.method() === 'POST') {
      const segments = pathname.split('/');
      const threadId = segments[segments.length - 2];
      return route.fulfill({
        json: {
          thread: agentThreadsData.find((thread) => thread.id === threadId) ?? agentThreadsData[0],
          messages: agentMessagesData[threadId] ?? agentMessagesData['thread-ops'],
        },
      });
    }

    if (pathname.includes('/api/v1/agents/profiles/') && pathname.endsWith('/runtime') && request.method() === 'PATCH') {
      const segments = pathname.split('/');
      const profileId = segments[segments.length - 2];
      const payload = request.postDataJSON() as { providerCode: string; modelCode: string; versionLabel?: string };
      const provider = providersData.find((item) => item.code === payload.providerCode) ?? providersData[0];
      const model = modelsData.find((item) => item.code === payload.modelCode) ?? modelsData[0];

      agentProfilesData = agentProfilesData.map((profile) => (
        profile.id !== profileId
          ? profile
          : {
              ...profile,
              providerCode: provider.code,
              modelCode: model.code,
              versionLabel: payload.versionLabel ?? model.versionLabel,
              apiStyle: provider.apiStyle,
              note: `Configure ${provider.credentialFields[0]?.envVar ?? 'API_KEY'} para ativar este runtime.`,
            }
      ));

      agentThreadsData = agentThreadsData.map((thread) => (
        thread.agentProfileId !== profileId
          ? thread
          : {
              ...thread,
              providerCode: provider.code,
              modelCode: model.code,
              versionLabel: payload.versionLabel ?? model.versionLabel,
              apiStyle: provider.apiStyle,
            }
      ));

      return route.fulfill({ json: agentProfilesData.find((profile) => profile.id === profileId) ?? agentProfilesData[0] });
    }

    if (pathname.endsWith('/api/search')) {
      return route.fulfill({
        json: [
          {
            id: 'task-onboarding',
            title: 'Estruturar onboarding operacional',
            description: 'Task assistida com contexto real.',
            path: '/tasks/task-onboarding',
            section: 'Workspace',
            availability: 'preview',
            keywords: ['onboarding'],
          },
        ],
      });
    }

    if (pathname.includes('/api/v1/workspaces/') && pathname.endsWith('/activate')) {
      const segments = pathname.split('/');
      const workspaceId = Number(segments[segments.length - 2]);
      activateWorkspace(workspaceId);
      return route.fulfill({ status: 204, body: '' });
    }

    return route.fulfill({ json: {} });
  });
});

test('home renders and the search modal opens with keyboard', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Um motor de tarefas com shell honesta, contexto real e visual de control room.')).toBeVisible();
  await page.keyboard.press('Control+K');
  await expect(page.getByText('Busca global do workspace')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByText('Busca global do workspace')).not.toBeVisible();
});

test('settings toggles the theme and persists the resolved theme on the root element', async ({ page }) => {
  await page.goto('/settings');
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  await page.getByRole('button', { name: 'Dark' }).click();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
});

test('workspace switch updates the topbar context without breaking layout', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('banner').getByText('Lume . Workspace Principal')).toBeVisible();
  await page.getByLabel('Selecionar workspace ativo').selectOption('2');
  await expect(page.getByLabel('Selecionar workspace ativo')).toHaveValue('2');
  await expect(page.getByRole('banner').getByText('Lume . Workspace Secundario')).toBeVisible();
});

test('home creates a task and opens the generated task view', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Prompt principal').fill('Gerar um roteiro de onboarding com checkpoints claros.');
  await page.getByRole('button', { name: 'Enviar' }).click();
  await expect(page).toHaveURL(/\/tasks\/task-generated$/);
  await expect(page.getByRole('heading', { name: 'Nova task gerada a partir da home' })).toBeVisible();
});

test('library filters entries with the local search field', async ({ page }) => {
  await page.goto('/library');
  await expect(page.getByText('Playbook de onboarding')).toBeVisible();
  await page.getByLabel('Buscar na biblioteca').fill('inexistente');
  await expect(page.getByText('Nada encontrado.')).toBeVisible();
  await page.getByLabel('Buscar na biblioteca').fill('onboarding');
  await expect(page.getByText('Playbook de onboarding')).toBeVisible();
});

test('members CRUD continues functional on the workspace shell', async ({ page }) => {
  await page.goto('/users');

  await page.getByRole('button', { name: 'Novo membro' }).click();
  await page.getByLabel('Nome').fill('Ana Ops');
  await page.getByLabel('Role').selectOption('workspace_member');
  await page.getByLabel('E-mail').fill('ana.ops@lume.local');
  await page.getByLabel(/Senha/).fill('segredo123');
  await page.getByRole('button', { name: 'Criar usuario' }).click();

  const createdCard = page.locator('article').filter({ hasText: 'Ana Ops' }).first();
  await expect(createdCard).toBeVisible();

  await createdCard.getByRole('button', { name: 'Editar' }).click();
  await page.getByLabel('Nome').fill('Ana Ops Lead');
  await page.getByLabel('Role').selectOption('workspace_admin');
  await page.getByRole('button', { name: 'Atualizar usuario' }).click();

  const updatedCard = page.locator('article').filter({ hasText: 'Ana Ops Lead' }).first();
  await expect(updatedCard).toBeVisible();
  await expect(updatedCard.getByText('Workspace Admin')).toBeVisible();

  await updatedCard.getByRole('button', { name: 'Desativar' }).click();
  await page.getByRole('button', { name: 'Desativar membership' }).click();
  await expect(updatedCard.getByText('Inativo')).toBeVisible();
});

test('preview sections stay visible but do not fire invalid preference updates', async ({ page }) => {
  let preferencePatchCount = 0;

  page.on('request', (request) => {
    if (request.url().includes('/api/v1/settings/preferences') && request.method() === 'PATCH') {
      preferencePatchCount += 1;
    }
  });

  await page.goto('/settings');
  await page.getByRole('button', { name: /Mail/ }).click();
  await expect(page.getByText('Preview visivel')).toBeVisible();
  await expect(page.getByText('Preview honesto e sem side effects')).toBeVisible();
  await page.waitForTimeout(300);
  expect(preferencePatchCount).toBe(0);
});

test('settings renders providers runtime and threat intelligence sections with real catalog metadata', async ({ page }) => {
  await page.goto('/settings?section=providers-runtime');
  await expect(page.getByRole('heading', { name: 'Providers & Runtime' })).toBeVisible();
  await expect(page.getByText('OpenAI', { exact: true })).toBeVisible();
  await expect(page.getByText('Responses API como caminho principal.')).toBeVisible();

  await page.getByRole('button', { name: /Threat Intelligence/ }).click();
  await expect(page.getByText('Preview honesto, admin-only e sem execucao silenciosa')).toBeVisible();
  await expect(page.getByText('DarkOwl', { exact: true })).toBeVisible();
});

test('agents page exposes runtime metadata and allows admin runtime updates', async ({ page }) => {
  await page.goto('/agents');
  await expect(page.getByText('Runtime real por perfil')).toBeVisible();
  await page.getByLabel('Selecionar provider do agent').selectOption('anthropic');
  await page.getByLabel('Selecionar modelo do agent').selectOption('anthropic:claude-sonnet-4-5');
  await page.getByLabel('Versao do agent').fill('agent-v2-claude');
  await page.getByRole('button', { name: /Salvar runtime/ }).click();
  await expect(page.locator('button').filter({ hasText: 'Ops Strategist' }).getByText(/^anthropic$/)).toBeVisible();
  await expect(page.getByText('agent-v2-claude').first()).toBeVisible();
});

test('core routes render with mocked backend contracts', async ({ page }) => {
  const routes = [
    ['/agents', 'Runtime real por perfil'],
    ['/tasks', 'Task board inspirado no Manus'],
    ['/tasks/task-onboarding', 'Task view em linguagem de execucao.'],
    ['/library', 'Biblioteca agora opera sobre dados reais do workspace.'],
    ['/projects', 'Projetos entram como modulo real do workspace.'],
    ['/users', 'Membros do workspace com tenancy real.'],
    ['/settings', 'Settings em formato modal-page.'],
  ] as const;

  for (const [route, heading] of routes) {
    await page.goto(route);
    await expect(page.getByText(heading)).toBeVisible();
  }
});

test('mobile sidebar opens from the menu button', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await page.getByRole('button', { name: 'Abrir menu lateral' }).click();
  await expect(page.getByLabel('Menu lateral do workspace').getByText('Historico de tarefas')).toBeVisible();
});
