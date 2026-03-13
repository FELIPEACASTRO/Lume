import { apiFetch } from "@/lib/api/http";
import {
  AuditFeedEntry,
  Invoice,
  FinopsAnomaly,
  FinopsScorecard,
  HomeOverviewResponse,
  KnowledgeSource,
  LibraryEntry,
  Member,
  ByokConnection,
  PaymentEvent,
  PurchaseCreditPackResponse,
  PromptTemplate,
  ProjectSummary,
  SearchResultsResponse,
  ShellNavigationResponse,
  SettingsOverview,
  SettingsPreferences,
  SupportTicket,
  TaskDetail,
  TaskSummary,
  UiOptionsResponse,
  UsageSummary,
  WorkspaceOnboarding,
  WorkspaceSubscription
} from "@/lib/api/types";

export function getShellNavigation(): Promise<ShellNavigationResponse> {
  return apiFetch<ShellNavigationResponse>("/v1/shell/navigation");
}

export function getHomeOverview(): Promise<HomeOverviewResponse> {
  return apiFetch<HomeOverviewResponse>("/v1/home/overview");
}

export function searchResults(query: string): Promise<SearchResultsResponse> {
  return apiFetch<SearchResultsResponse>(`/v1/search/results?q=${encodeURIComponent(query)}`);
}

export function listTasks(projectId?: string): Promise<TaskSummary[]> {
  const query = projectId ? `?project=${encodeURIComponent(projectId)}` : "";
  return apiFetch<TaskSummary[]>(`/tasks${query}`);
}

export function createTask(payload: {
  prompt: string;
  taskType: string;
  projectId?: string;
  providerCode?: string;
  modelCode?: string;
  versionLabel?: string;
}): Promise<TaskDetail> {
  return apiFetch<TaskDetail>("/tasks", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getTask(taskId: string): Promise<TaskDetail> {
  return apiFetch<TaskDetail>(`/tasks/${encodeURIComponent(taskId)}`);
}

export function listProjects(): Promise<ProjectSummary[]> {
  return apiFetch<ProjectSummary[]>("/v1/projects");
}

export function createProject(payload: {
  name: string;
  summary: string;
  statusLabel?: string;
  availability?: string;
  ownerName?: string;
}): Promise<ProjectSummary> {
  return apiFetch<ProjectSummary>("/v1/projects", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateProject(
  projectId: string,
  payload: {
    name?: string;
    summary?: string;
    statusLabel?: string;
    availability?: string;
    ownerName?: string;
  }
): Promise<ProjectSummary> {
  return apiFetch<ProjectSummary>(`/v1/projects/${encodeURIComponent(projectId)}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function listMembers(): Promise<Member[]> {
  return apiFetch<Member[]>("/v1/members");
}

export function createMember(payload: {
  name: string;
  email: string;
  password: string;
  roleCode: string;
}): Promise<Member> {
  return apiFetch<Member>("/v1/members", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateMember(
  memberId: number,
  payload: {
    name?: string;
    email?: string;
    password?: string;
    roleCode?: string;
    active?: boolean;
  }
): Promise<Member> {
  return apiFetch<Member>(`/v1/members/${memberId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function listKnowledgeSources(): Promise<KnowledgeSource[]> {
  return apiFetch<KnowledgeSource[]>("/v1/knowledge-sources");
}

export function createKnowledgeSource(payload: {
  title: string;
  sourceType: string;
  sourceUri?: string;
  projectId?: string | null;
  documentCount?: number;
  enabledForAgents?: boolean;
  statusLabel?: string;
  availability?: string;
  note: string;
}): Promise<KnowledgeSource> {
  return apiFetch<KnowledgeSource>("/v1/knowledge-sources", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateKnowledgeSource(
  sourceId: string,
  payload: {
    title?: string;
    sourceType?: string;
    sourceUri?: string;
    projectId?: string | null;
    documentCount?: number;
    enabledForAgents?: boolean;
    statusLabel?: string;
    availability?: string;
    note?: string;
  }
): Promise<KnowledgeSource> {
  return apiFetch<KnowledgeSource>(`/v1/knowledge-sources/${encodeURIComponent(sourceId)}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function listLibraryEntries(): Promise<LibraryEntry[]> {
  return apiFetch<LibraryEntry[]>("/v1/library/entries");
}

export function listPromptTemplates(): Promise<PromptTemplate[]> {
  return apiFetch<PromptTemplate[]>("/v1/prompt-templates");
}

export function createPromptTemplate(payload: {
  title: string;
  summary: string;
  promptBody: string;
  templateScope?: string;
  projectId?: string | null;
  agentProfileId?: string | null;
  variables?: string[];
  favorited?: boolean;
  statusLabel?: string;
  availability?: string;
}): Promise<PromptTemplate> {
  return apiFetch<PromptTemplate>("/v1/prompt-templates", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updatePromptTemplate(
  templateId: string,
  payload: {
    title?: string;
    summary?: string;
    promptBody?: string;
    templateScope?: string;
    projectId?: string | null;
    agentProfileId?: string | null;
    variables?: string[];
    favorited?: boolean;
    statusLabel?: string;
    availability?: string;
  }
): Promise<PromptTemplate> {
  return apiFetch<PromptTemplate>(`/v1/prompt-templates/${encodeURIComponent(templateId)}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function touchPromptTemplate(templateId: string): Promise<PromptTemplate> {
  return apiFetch<PromptTemplate>(`/v1/prompt-templates/${encodeURIComponent(templateId)}/touch`, {
    method: "POST"
  });
}

export function deletePromptTemplate(templateId: string): Promise<void> {
  return apiFetch<void>(`/v1/prompt-templates/${encodeURIComponent(templateId)}`, {
    method: "DELETE"
  });
}

export function getCurrentOnboarding(): Promise<WorkspaceOnboarding> {
  return apiFetch<WorkspaceOnboarding>("/v1/onboarding/current");
}

export function updateCurrentOnboarding(payload: {
  primaryUseCase?: string;
  workStyle?: string;
  activationStatus?: string;
  activationNote?: string;
}): Promise<WorkspaceOnboarding> {
  return apiFetch<WorkspaceOnboarding>("/v1/onboarding/current", {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function getSettingsOverview(): Promise<SettingsOverview> {
  return apiFetch<SettingsOverview>("/v1/settings/overview");
}

export function getSettingsUiOptions(): Promise<UiOptionsResponse> {
  return apiFetch<UiOptionsResponse>("/v1/settings/ui-options");
}

export function getSettingsPreferences(): Promise<SettingsPreferences> {
  return apiFetch<SettingsPreferences>("/v1/settings/preferences");
}

export function updateSettingsPreferences(payload: {
  appearance?: string;
  languageCode?: string;
  emailUpdates?: boolean;
  productUpdates?: boolean;
}): Promise<SettingsPreferences> {
  return apiFetch<SettingsPreferences>("/v1/settings/preferences", {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function getSettingsCompliance(): Promise<SettingsOverview["compliance"]> {
  return apiFetch<SettingsOverview["compliance"]>("/v1/settings/compliance");
}

export function updateSettingsCompliance(payload: {
  retentionPolicyStatus?: string;
  retentionDays?: number | null;
  accessReviewStatus?: string;
  accessReviewFrequencyDays?: number | null;
  consentTrackingEnabled?: boolean;
  termsVersion?: string | null;
}): Promise<SettingsOverview["compliance"]> {
  return apiFetch<SettingsOverview["compliance"]>("/v1/settings/compliance", {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function getAuditFeed(limit = 20): Promise<AuditFeedEntry[]> {
  const safeLimit = Math.max(1, Math.min(200, limit));
  return apiFetch<AuditFeedEntry[]>(`/v1/settings/audit-feed?limit=${safeLimit}`);
}

export function getSubscription(): Promise<WorkspaceSubscription> {
  return apiFetch<WorkspaceSubscription>("/v1/billing/subscription");
}

export function listInvoices() {
  return apiFetch<Invoice[]>("/v1/billing/invoices");
}

export function listPaymentEvents() {
  return apiFetch<PaymentEvent[]>("/v1/billing/payment-events");
}

export function purchaseCreditPack(payload: {
  packCode: string;
  credits: number;
  amountBrl: number;
  description?: string;
}): Promise<PurchaseCreditPackResponse> {
  return apiFetch<PurchaseCreditPackResponse>("/v1/billing/credit-packs/purchase", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getUsageSummary(): Promise<UsageSummary> {
  return apiFetch<UsageSummary>("/usage/summary");
}

export function getFinopsScorecard(): Promise<FinopsScorecard> {
  return apiFetch<FinopsScorecard>("/v1/finops/scorecard");
}

export function listFinopsAnomalies(): Promise<FinopsAnomaly[]> {
  return apiFetch<FinopsAnomaly[]>("/v1/finops/anomalies");
}

export function listSupportTickets(limit = 20): Promise<SupportTicket[]> {
  return apiFetch<SupportTicket[]>(`/v1/support/tickets?limit=${Math.max(1, Math.min(200, limit))}`);
}

export function createSupportTicket(payload: {
  title: string;
  description: string;
  category?: string;
  severity?: string;
}): Promise<SupportTicket> {
  return apiFetch<SupportTicket>("/v1/support/tickets", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateSupportTicket(
  ticketId: string,
  payload: {
    status?: string;
    resolutionNote?: string;
    severity?: string;
    category?: string;
  }
): Promise<SupportTicket> {
  return apiFetch<SupportTicket>(`/v1/support/tickets/${encodeURIComponent(ticketId)}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function listByokConnections(): Promise<ByokConnection[]> {
  return apiFetch<ByokConnection[]>("/v1/byok/connections");
}

export function createByokConnection(payload: {
  providerCode: string;
  connectionName: string;
  secretRef: string;
  scopeLabel?: string;
}): Promise<ByokConnection> {
  return apiFetch<ByokConnection>("/v1/byok/connections", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function validateByokConnection(connectionId: string): Promise<ByokConnection> {
  return apiFetch<ByokConnection>(`/v1/byok/connections/${encodeURIComponent(connectionId)}/validate`, {
    method: "POST"
  });
}
