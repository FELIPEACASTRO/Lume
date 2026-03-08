export interface User {
  id: number;
  membershipId?: number;
  userId?: number;
  name: string;
  email: string;
  active: boolean;
  roleCode?: string;
  roleLabel?: string;
  currentUser?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserRequest {
  name: string;
  email: string;
  password: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  details?: Record<string, string>;
}

export interface ApiClientError {
  status?: number;
  code?: string;
  message: string;
  details?: Record<string, string>;
}

export type PreviewState = 'live' | 'preview' | 'disabled-preview';
export type WorkspaceDataState = PreviewState | 'loading' | 'empty' | 'error';
export type ThemeMode = 'light' | 'dark';
export type WorkspaceGroup = 'primary' | 'task-history' | 'secondary';

export type ShellIconKey =
  | 'home'
  | 'users'
  | 'agents'
  | 'library'
  | 'projects'
  | 'tasks'
  | 'search'
  | 'inbox'
  | 'usage'
  | 'settings';

export interface ShellNavItem {
  label: string;
  path: string;
  description: string;
  icon: ShellIconKey;
  availability: WorkspaceDataState;
  group: WorkspaceGroup;
  keywords: string[];
  badge?: string;
}

export interface QuickAction {
  id: string;
  title: string;
  description: string;
  prompt: string;
  taskType: string;
}

export interface RecentTask {
  id: string;
  title: string;
  summary: string;
  detail: string;
  path: string;
  availability: WorkspaceDataState;
}

export interface WorkspaceFacet {
  id: string;
  label: string;
  headline: string;
  description: string;
  availability: WorkspaceDataState;
  path?: string;
}

export interface LibraryEntry {
  id: string;
  title: string;
  category: string;
  status: string;
  availability: WorkspaceDataState;
  owner: string;
  sourceLabel: string;
  summary: string;
  tags: string[];
}

export interface AgentProfile {
  id: string;
  name: string;
  specialty: string;
  description: string;
  status: string;
  availability: WorkspaceDataState;
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
}

export interface AgentMessage {
  id: string;
  role: 'assistant' | 'user' | 'system';
  body: string;
  timestamp: string;
}

export interface SearchTarget {
  id: string;
  title: string;
  description: string;
  path: string;
  section: string;
  availability: WorkspaceDataState;
  keywords: string[];
}

export interface SessionContext {
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
}

export interface WorkspaceOptionDto {
  id: number;
  name: string;
  slug: string;
  organizationId: number;
  organizationName: string;
  roleCode: string;
  roleLabel: string;
  active: boolean;
}

export interface WorkspaceSummary {
  workspaceName: string;
  organizationName: string;
  counts: {
    users: number;
    libraryEntries: number;
    agentThreads: number;
    projects: number;
    tasks: number;
    unreadNotifications: number;
  };
  recentItems: RecentTask[];
  workspaceFacets: WorkspaceFacet[];
}

export interface AgentThread {
  id: string;
  agentProfileId: string;
  agentName: string;
  title: string;
  status: string;
  availability: WorkspaceDataState;
  lastMessagePreview: string;
  updatedAt: string;
  providerCode?: string | null;
  modelCode?: string | null;
  versionLabel?: string | null;
  apiStyle?: string | null;
  credentialState?: string | null;
  catalogState?: string | null;
}

export interface AgentConversation {
  thread: AgentThread;
  messages: AgentMessage[];
}

export interface ProjectDto {
  id: string;
  name: string;
  summary: string;
  statusLabel: string;
  availability: WorkspaceDataState;
  ownerName: string;
  taskCount: number;
  updatedAt: string;
}

export interface TaskSummaryDto {
  id: string;
  projectId: string | null;
  projectName: string | null;
  taskType: string;
  title: string;
  prompt: string;
  summary: string;
  statusLabel: string;
  availability: WorkspaceDataState;
  ownerName: string;
  updatedAt: string;
  scheduledFor: string | null;
  shareSlug: string | null;
}

export interface TaskStepDto {
  id: string;
  stepOrder: number;
  stepType: string;
  title: string;
  detail: string;
  statusLabel: string;
}

export interface TaskDetailDto {
  task: TaskSummaryDto;
  steps: TaskStepDto[];
  followUpSuggestions: string[];
}

export interface CreateTaskRequest {
  prompt: string;
  taskType: string;
  projectId?: string;
}

export interface NotificationDto {
  id: string;
  kind: string;
  title: string;
  body: string;
  path: string;
  read: boolean;
  createdAt: string;
}

export interface UsageSummaryDto {
  dailyCredits: number;
  consumedCredits: number;
  remainingCredits: number;
  activeTasks: number;
  scheduledTasks: number;
  unreadNotifications: number;
  note: string;
}

export interface SettingsPreferencesDto {
  appearance: ThemeMode;
  languageCode: string;
  emailUpdates: boolean;
  productUpdates: boolean;
}

export interface SettingsSectionDto {
  key: string;
  title: string;
  description: string;
  availability: WorkspaceDataState;
  previewState: PreviewState;
}

export interface SettingsOverviewDto {
  organizationName: string;
  workspaceName: string;
  roleLabel: string;
  unreadNotifications: number;
  knowledgeSources: number;
  usage: UsageSummaryDto;
  preferences: SettingsPreferencesDto;
  sections: SettingsSectionDto[];
}

export interface CredentialFieldDto {
  key: string;
  label: string;
  envVar: string;
  required: boolean;
  secret: boolean;
  configured: boolean;
  description: string;
}

export interface ProviderDto {
  code: string;
  name: string;
  category: string;
  protocol: string;
  apiStyle: string;
  executionSupported: boolean;
  configured: boolean;
  adminOnly: boolean;
  tenantScoped: boolean;
  supportsResponsesApi: boolean;
  supportsChatCompletions: boolean;
  streamingSupported: boolean;
  catalogState: string;
  requiredHeaders: string[];
  credentialFields: CredentialFieldDto[];
  apiKeyPortalUrl: string;
  docsUrl: string;
  defaultModelCode: string;
  capabilities: string[];
  notes: string;
}

export interface ProviderStatusDto {
  providerCode: string;
  providerName: string;
  configured: boolean;
  executionSupported: boolean;
  catalogState: string;
  category: string;
  adminOnly: boolean;
  streamingSupported: boolean;
  readinessStatus: string;
  missingCredentialEnvVars: string[];
}

export interface ProviderCredentialDto {
  providerCode: string;
  providerName: string;
  configured: boolean;
  executionSupported: boolean;
  category: string;
  apiStyle: string;
  adminOnly: boolean;
  streamingSupported: boolean;
  catalogState: string;
  missingCredentialEnvVars: string[];
  credentialFields: CredentialFieldDto[];
  apiKeyPortalUrl: string;
  docsUrl: string;
}

export interface ProviderConnectivityDto {
  providerCode: string;
  providerName: string;
  category: string;
  apiStyle: string;
  status: string;
  configured: boolean;
  executionSupported: boolean;
  streamingSupported: boolean;
  latencyMs?: number | null;
  message: string;
  missingCredentialEnvVars: string[];
}

export interface ProviderHealthDto {
  providerCode: string;
  providerName: string;
  category: string;
  configured: boolean;
  executionSupported: boolean;
  streamingSupported: boolean;
  readinessStatus: string;
  message: string;
  lastConnectivityStatus?: string | null;
  lastCheckedAt?: string | null;
  missingCredentialEnvVars: string[];
}

export interface ModelDto {
  code: string;
  providerCode: string;
  label: string;
  versionLabel: string;
  apiStyle: string;
  catalogState: string;
  defaultModel: boolean;
  enabledForAgents: boolean;
}

export interface UpdateAgentRuntimeRequest {
  providerCode: string;
  modelCode?: string;
  versionLabel?: string;
  systemPrompt?: string;
}

export interface CreateAgentThreadRequest {
  agentProfileId: string;
  message: string;
}

export interface CreateAgentMessageRequest {
  message: string;
}

export interface UserCreateRequest {
  name: string;
  email: string;
  password: string;
  roleCode: string;
}

export interface UserUpdateRequest {
  name: string;
  email: string;
  password?: string;
  roleCode: string;
  active?: boolean;
}

export interface UserFormData {
  name: string;
  email: string;
  password: string;
  roleCode: string;
}
