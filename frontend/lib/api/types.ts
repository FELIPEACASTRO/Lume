export type SetupStatus = {
  setupRequired: boolean;
  organizations: number;
  workspaces: number;
  users: number;
};

export type SessionContext = {
  user: {
    id: number;
    name: string;
    email: string;
    initials: string;
  };
  organization: {
    id: number;
    name: string;
    slug: string;
  };
  workspace: {
    id: number;
    name: string;
    slug: string;
  };
  role: {
    code: string;
    label: string;
    permissions: string[];
  };
};

export type WorkspaceOnboarding = {
  primaryUseCase: string;
  workStyle: string;
  activationStatus: OnboardingActivationStatus;
  activationNote: string;
};

export type OnboardingActivationStatus =
  | "started"
  | "profile_selected"
  | "first_project_created"
  | "first_task_created"
  | "first_prompt_sent"
  | "completed";

export type ShellNavigationResponse = {
  items: Array<{
    id: string;
    label: string;
    path: string;
    description: string;
    icon: string;
    availability: string;
    group: string;
    keywords: string[];
  }>;
  taskTypes: Array<{
    taskType: string;
    label: string;
    description: string;
  }>;
};

export type HomeOverviewResponse = {
  workspaceName: string;
  organizationName: string;
  headline: string;
  supportingText: string;
  blocks: Array<{
    id: string;
    blockType: string;
    title: string;
    description: string;
    sortOrder: number;
    maxItems: number;
    ctaLabel: string;
    ctaPath: string;
    enabled: boolean;
  }>;
  inProgress: Array<{
    id: string;
    title: string;
    summary: string;
    statusLabel: string;
    runtimeState: string;
    ownerName: string;
    path: string;
  }>;
  recentItems: Array<{
    id: string;
    title: string;
    summary: string;
    detail: string;
    path: string;
    availability: string;
  }>;
  alerts: Array<{
    id: string;
    title: string;
    body: string;
    kind: string;
    path: string;
    createdAt: string;
    read: boolean;
  }>;
  teamAndContext: Array<{
    id: string;
    label: string;
    value: string;
    detail: string;
    availability: string;
    path: string;
  }>;
};

export type SearchResultsResponse = {
  query: string;
  totalResults: number;
  results: Array<{
    id: string;
    title: string;
    description: string;
    path: string;
    section: string;
    availability: string;
    keywords: string[];
  }>;
};

export type ProviderStatus = {
  providerCode: string;
  providerName: string;
  configured: boolean;
  executionSupported: boolean;
  implementationStatus: string;
  evidenceLevel: string;
  catalogState: string;
  category: string;
  adminOnly: boolean;
  streamingMode: string;
  providerTier: string;
  runtimeMaturity: string;
  readinessStatus: string;
  smokeStatus: string;
  blockerCode: string | null;
  blockerMessage: string | null;
  missingCredentialEnvVars: string[];
};

export type ProviderReadiness = {
  providerCode: string;
  providerName: string;
  category: string;
  configured: boolean;
  executionSupported: boolean;
  providerTier: string;
  readinessStatus: string;
  runtimeMaturity: string;
  implementationStatus: string;
  evidenceLevel: string;
  smokeStatus: string;
  blockerCode: string | null;
  blockerMessage: string | null;
  adminOnly: boolean;
  missingCredentialEnvVars: string[];
};

export type ModelResponse = {
  code: string;
  providerCode: string;
  label: string;
  versionLabel: string;
  apiStyle: string;
  catalogState: string;
  defaultModel: boolean;
  enabledForAgents: boolean;
};

export type ExecutionSetTarget = {
  providerCode: string;
  modelCode: string;
  versionLabel?: string | null;
};

export type MultiInferenceRequest = {
  executionSet: ExecutionSetTarget[];
  systemPrompt?: string;
  prompt?: string;
  messages?: Array<{ role: string; content: string }>;
  temperature?: number;
  maxTokens?: number;
  rankingPolicy?: "quality-first" | "cost-first" | "latency-first";
  routingPolicy?: "balanced" | "cost-first" | "latency-first" | "quality-first";
  requestId?: string;
  tags?: string[];
  workspaceId?: string;
};

export type MultiInferenceRun = {
  providerCode: string;
  providerName: string;
  modelCode: string;
  versionLabel: string;
  status: string;
  content: string | null;
  error: string | null;
  fallbackUsed: boolean;
  latencyMs: number | null;
  estimatedInputTokens: number | null;
  estimatedOutputTokens: number | null;
  estimatedCostUsd: number | null;
  score: number | null;
  rankingReasons: string[];
};

export type MultiInferenceCostSummary = {
  totalEstimatedCostUsd: number | null;
  averageEstimatedCostUsd: number | null;
  completedRuns: number;
  failedRuns: number;
};

export type MultiInferenceResult = {
  winner: MultiInferenceRun | null;
  runs: MultiInferenceRun[];
  rankingReasons: string[];
  costSummary: MultiInferenceCostSummary;
  policyDecisionSummary: string;
  rankingPolicy: string;
  routingPolicy: string;
};

export type TaskSummary = {
  id: string;
  projectId: string | null;
  projectName: string | null;
  taskType: string;
  title: string;
  prompt: string;
  summary: string;
  statusLabel: string;
  availability: string;
  runtimeState: string;
  lastError: string | null;
  ownerName: string;
  updatedAt: string;
  scheduledFor: string | null;
  shareSlug: string | null;
  providerCode: string | null;
  modelCode: string | null;
  versionLabel: string | null;
};

export type TaskStep = {
  id: string;
  stepOrder: number;
  stepType: string;
  title: string;
  detail: string;
  statusLabel: string;
};

export type TaskDetail = {
  task: TaskSummary;
  steps: TaskStep[];
  followUpSuggestions: string[];
};

export type ProjectSummary = {
  id: string;
  name: string;
  summary: string;
  statusLabel: string;
  availability: string;
  ownerName: string;
  taskCount: number;
  updatedAt: string;
};

export type Member = {
  id: number;
  userId: number;
  name: string;
  email: string;
  active: boolean;
  roleCode: string;
  roleLabel: string;
  createdAt: string;
  currentUser: boolean;
};

export type KnowledgeSource = {
  id: string;
  title: string;
  sourceType: string;
  sourceUri: string;
  projectId: string | null;
  projectName: string | null;
  statusLabel: string;
  availability: string;
  documentCount: number;
  enabledForAgents: boolean;
  note: string;
  lastIndexedAt: string | null;
  updatedAt: string;
};

export type LibraryEntry = {
  id: string;
  title: string;
  category: string;
  entryType: string;
  status: string;
  availability: string;
  owner: string;
  sourceLabel: string;
  summary: string;
  tags: string[];
  projectId: string | null;
  projectName: string | null;
  favorited: boolean;
  archived: boolean;
  versionCount: number;
  currentVersionLabel: string | null;
};

export type PromptTemplate = {
  id: string;
  title: string;
  summary: string;
  promptBody: string;
  variables: string[];
  templateScope: string;
  statusLabel: string;
  availability: string;
  ownerName: string;
  projectId: string | null;
  projectName: string | null;
  agentProfileId: string | null;
  agentProfileName: string | null;
  favorited: boolean;
  lastUsedAt: string | null;
  updatedAt: string;
};

export type WorkspaceSubscription = {
  planCode: string;
  planLabel: string;
  subscriptionStatus: string;
  billingInterval: string;
  includedCredits: number;
  extraCredits: number;
  totalCredits: number;
  renewsAt: string | null;
  commercialNote: string;
};

export type PurchaseCreditPackResponse = {
  subscription: WorkspaceSubscription;
  invoice: Invoice;
  paymentEvent: PaymentEvent;
  checkoutUrl: string | null;
  paymentStatus: string | null;
  externalReference: string | null;
};

export type Invoice = {
  id: number;
  invoiceNumber: string;
  status: string;
  amountBrl: number;
  currency: string;
  dueAt: string | null;
  paidAt: string | null;
  description: string;
  createdAt: string;
};

export type PaymentEvent = {
  id: number;
  invoiceId: number | null;
  gatewayEventId: string;
  eventType: string;
  status: string;
  amountBrl: number;
  occurredAt: string;
  processedAt: string | null;
};

export type UsageSummary = {
  dailyCredits: number;
  consumedCredits: number;
  remainingCredits: number;
  activeTasks: number;
  scheduledTasks: number;
  unreadNotifications: number;
  note: string;
  budget: {
    costCenter: string;
    chargebackMode: string;
    softLimitCredits: number;
    hardLimitCredits: number;
    consumedCredits: number;
    remainingSoftCredits: number;
    remainingHardCredits: number;
    softLimitUtilizationPercent: number;
    hardLimitUtilizationPercent: number;
    softLimitReached: boolean;
    hardLimitReached: boolean;
    budgetStatus: string;
    note: string;
  };
  commercial: {
    planCode: string;
    planLabel: string;
    subscriptionStatus: string;
    billingInterval: string;
    includedCredits: number;
    extraCredits: number;
    totalCredits: number;
    renewsAt: string | null;
    primaryUseCase: string;
    workStyle: string;
    activationStatus: string;
    activationNote: string;
    commercialNote: string;
  };
};

export type FinopsScorecard = {
  activationMinutesToFirstTaskMedian: number | null;
  d30RetentionRate: number | null;
  churnRate: number | null;
  marginContributionRate: number | null;
  operationalNps: number | null;
  weeklyActiveUsers: number | null;
  monthlyActiveUsers: number | null;
  taskCompletionRate: number | null;
  inferenceSuccessRate: number | null;
  averageEstimatedCostUsdPerRun: number | null;
  paidInvoicesCount: number;
  paymentFailureCount: number;
  orphanPaymentEvents: number;
  pendingPaymentEvents: number;
  reconciliationStatus: string | null;
  creditDrift: number | null;
  lastReconciledAt: string | null;
  openSupportTickets: number;
  criticalOpenSupportTickets: number;
  overdueSupportTickets: number;
  byokConnections: number;
  healthyByokConnections: number;
  coreLiveProviders: number;
  supportedRestrictedProviders: number;
  blockedProviders: number;
  currentCreditBalance: number;
  notes: string;
};

export type FinopsAnomaly = {
  code: string;
  severity: "warning" | "critical" | "info" | string;
  status: string;
  title: string;
  detail: string;
  recommendedAction: string;
  detectedAt: string;
};

export type SupportTicket = {
  id: string;
  title: string;
  description: string;
  category: string;
  severity: string;
  status: string;
  resolutionNote: string | null;
  createdByUserId: number | null;
  slaTargetAt: string | null;
  slaBreached: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ByokConnection = {
  id: string;
  providerCode: string;
  providerName: string;
  connectionName: string;
  secretRef: string;
  scopeLabel: string;
  status: string;
  healthStatus: string;
  lastValidatedAt: string | null;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
};

export type SettingsPreferences = {
  appearance: string;
  languageCode: string;
  emailUpdates: boolean;
  productUpdates: boolean;
};

export type SettingsSection = {
  key: string;
  title: string;
  description: string;
  availability: string;
};

export type SettingsGovernanceSummary = {
  openSupportTickets: number;
  criticalOpenSupportTickets: number;
  overdueSupportTickets: number;
  byokConnections: number;
  healthyByokConnections: number;
  coreLiveProviders: number;
  supportedRestrictedProviders: number;
  blockedProviders: number;
  lastReconciliationStatus: string | null;
  lastReconciliationExecutedAt: string | null;
  note: string;
};

export type SettingsComplianceSummary = {
  billingWebhookSecretConfigured: boolean;
  auditTrailEnabled: boolean;
  darkWebMonitoringEnabled: boolean;
  threatIntelRestrictedToAdmins: boolean;
  byokValidationEnabled: boolean;
  retentionPolicyStatus: string;
  retentionDays: number | null;
  accessReviewStatus: string;
  accessReviewFrequencyDays: number | null;
  consentTrackingEnabled: boolean;
  termsVersion: string | null;
  updatedAt: string | null;
  note: string;
};

export type AuditFeedEntry = {
  id: number;
  actorUserId: number | null;
  entityType: string;
  entityId: string;
  action: string;
  payload: string | null;
  createdAt: string | null;
};

export type SettingsOverview = {
  organizationName: string;
  workspaceName: string;
  roleLabel: string;
  unreadNotifications: number;
  knowledgeSources: number;
  usage: UsageSummary;
  commercial: UsageSummary["commercial"];
  preferences: SettingsPreferences;
  governance: SettingsGovernanceSummary;
  compliance: SettingsComplianceSummary;
  sections: SettingsSection[];
};

export type UiOptionItem = {
  code: string;
  label: string;
  description: string | null;
  defaultOption: boolean;
};

export type UiCreditPackOption = {
  packCode: string;
  credits: number;
  amountBrl: number;
  description: string;
  defaultOption: boolean;
};

export type UiByokProviderOption = {
  providerCode: string;
  providerName: string;
  secretRefSuggestion: string;
};

export type UiOptionsResponse = {
  onboardingPrimaryUseCases: UiOptionItem[];
  onboardingWorkStyles: UiOptionItem[];
  memberRoles: UiOptionItem[];
  supportCategories: UiOptionItem[];
  supportSeverities: UiOptionItem[];
  supportStatuses: UiOptionItem[];
  knowledgeSourceTypes: UiOptionItem[];
  billingCreditPacks: UiCreditPackOption[];
  byokProviders: UiByokProviderOption[];
  byokScopeOptions: UiOptionItem[];
  complianceRetentionPolicyStatuses: UiOptionItem[];
  complianceAccessReviewStatuses: UiOptionItem[];
};

export type ChatResponse = {
  providerCode: string;
  providerName: string;
  modelCode: string;
  requestedProviderCode: string;
  providerUsed: string;
  modelUsed: string;
  status: string;
  content: string;
  error: string | null;
  usage: {
    inputTokens: number;
    outputTokens: number;
    totalTokens: number;
    estimatedCostUsd: number;
  } | null;
  attemptChain: Array<{
    providerCode: string;
    status: string;
    error: string | null;
    latencyMs: number | null;
  }> | null;
};

export type AgentProfile = {
  id: string;
  name: string;
  specialty: string;
  description: string;
  statusLabel: string;
  availability: string;
  note: string;
  providerCode: string;
  modelCode: string;
  versionLabel: string;
  apiStyle: string;
  credentialState: string;
  catalogState: string;
  configured: boolean;
  executionSupported: boolean;
  toolset: string[];
};

export type AgentThread = {
  id: string;
  agentProfileId: string;
  agentName: string;
  title: string;
  statusLabel: string;
  availability: string;
  runtimeState: string;
  lastError: string | null;
  lastMessagePreview: string | null;
  updatedAt: string;
  providerCode: string | null;
  modelCode: string | null;
  versionLabel: string | null;
  apiStyle: string | null;
  credentialState: string | null;
  catalogState: string | null;
};

export type AgentMessage = {
  id: string;
  role: "user" | "assistant";
  body: string;
  createdAt: string;
};

export type AgentConversation = {
  thread: AgentThread;
  messages: AgentMessage[];
};
