import { useEffect, useMemo, useState } from 'react';
import {
  FiCheckCircle,
  FiChevronRight,
  FiDatabase,
  FiEdit3,
  FiExternalLink,
  FiMail,
  FiMoon,
  FiRefreshCcw,
  FiSettings,
  FiShield,
  FiSun,
  FiTrash2,
} from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { useTheme } from '../components/theme/ThemeProvider';
import { toApiClientError } from '../services/api';
import { budgetService } from '../services/budgetService';
import { homeService } from '../services/homeService';
import { knowledgeSourceService } from '../services/knowledgeSourceService';
import { providerService } from '../services/providerService';
import { projectService } from '../services/projectService';
import { shellCatalogService } from '../services/shellCatalogService';
import { settingsService } from '../services/settingsService';
import {
  BudgetSummaryDto,
  ChargebackMode,
  CreateHomeOverviewBlockRequest,
  CreateShellCatalogItemRequest,
  CreateShellTaskTypeRequest,
  CreateKnowledgeSourceRequest,
  HomeOverviewBlockDto,
  HomeOverviewBlockType,
  HomeOverviewCatalogDto,
  KnowledgeSourceDto,
  ProjectDto,
  ProviderConnectivityDto,
  ProviderCredentialDto,
  ProviderDto,
  ProviderHealthDto,
  ProviderStatusDto,
  ShellCatalogDto,
  ShellCatalogItemDto,
  ShellCatalogTaskTypeDto,
  ShellIconKey,
  SettingsOverviewDto,
  ThemeMode,
  UpdateHomeOverviewSettingsRequest,
  UpdateKnowledgeSourceRequest,
  WorkspaceAvailabilityState,
  WorkspaceGroup,
} from '../types';
import { humanizeChargebackMode, humanizeToken } from '../utils/uiText';

type ProviderCard = {
  provider: ProviderDto;
  status?: ProviderStatusDto;
  credentials?: ProviderCredentialDto;
  health?: ProviderHealthDto;
  connectivity?: ProviderConnectivityDto;
};

const emptyKnowledgeDraft: CreateKnowledgeSourceRequest = {
  title: '',
  sourceType: 'library',
  projectId: '',
  sourceUri: '',
  documentCount: 0,
  enabledForAgents: true,
  note: '',
};

function providerState(card: ProviderCard): WorkspaceAvailabilityState {
  if (!(card.health?.executionSupported ?? card.status?.executionSupported ?? card.provider.executionSupported)) {
    return card.provider.adminOnly ? 'restricted' : 'unavailable';
  }
  return (card.health?.configured ?? card.status?.configured ?? card.provider.configured) ? 'live' : 'attention';
}

function credentialSummary(card: ProviderCard) {
  const missing = card.credentials?.missingCredentialEnvVars ?? card.status?.missingCredentialEnvVars ?? [];
  if (missing.length === 0) {
    return 'Todas as credenciais obrigatorias estao presentes no ambiente.';
  }
  return `Faltando: ${missing.join(', ')}`;
}

function providerReadiness(card: ProviderCard) {
  const readiness = card.health?.readinessStatus ?? card.status?.readinessStatus ?? card.provider.catalogState;
  if (!readiness) {
    return 'Prontidao indisponivel.';
  }
  return humanizeToken(readiness);
}

function providerStreamingMode(card: ProviderCard) {
  return humanizeToken(card.health?.streamingMode ?? card.status?.streamingMode ?? card.provider.streamingMode);
}

function providerRuntimeMaturity(card: ProviderCard) {
  return humanizeToken(card.health?.runtimeMaturity ?? card.status?.runtimeMaturity ?? card.credentials?.runtimeMaturity ?? card.provider.runtimeMaturity);
}

function providerImplementationStatus(card: ProviderCard) {
  return humanizeToken(card.health?.implementationStatus ?? card.status?.implementationStatus ?? card.credentials?.implementationStatus ?? card.provider.implementationStatus);
}

function providerEvidenceLevel(card: ProviderCard) {
  return humanizeToken(card.health?.evidenceLevel ?? card.status?.evidenceLevel ?? card.credentials?.evidenceLevel ?? card.provider.evidenceLevel);
}

function ExternalLink({ href, label }: { href: string; label: string }) {
  return (
    <a className="pill-button" href={href} target="_blank" rel="noreferrer">
      {label}
      <FiExternalLink size={14} />
    </a>
  );
}

const shellIconOptions: Array<{ value: ShellIconKey; label: string }> = [
  { value: 'home', label: 'Inicio' },
  { value: 'tasks', label: 'Tarefas' },
  { value: 'projects', label: 'Projetos' },
  { value: 'library', label: 'Biblioteca' },
  { value: 'users', label: 'Equipe' },
  { value: 'settings', label: 'Configuracoes' },
  { value: 'search', label: 'Busca' },
  { value: 'usage', label: 'Uso' },
  { value: 'agents', label: 'Agentes' },
  { value: 'inbox', label: 'Entrada' },
];

const emptyShellCatalogItemDraft: CreateShellCatalogItemRequest = {
  id: '',
  label: '',
  path: '',
  description: '',
  icon: 'home',
  availability: 'live',
  group: 'secondary',
  sortOrder: 100,
  enabled: true,
  keywords: [],
};

const emptyShellTaskTypeDraft: CreateShellTaskTypeRequest = {
  taskType: '',
  label: '',
  description: '',
  sortOrder: 100,
  enabled: true,
};

const emptyHomeSettingsDraft: UpdateHomeOverviewSettingsRequest = {
  headline: '',
  supportingText: '',
};

const emptyHomeBlockDraft: CreateHomeOverviewBlockRequest = {
  id: '',
  blockType: 'in_progress',
  title: '',
  description: '',
  sortOrder: 100,
  maxItems: 4,
  ctaLabel: '',
  ctaPath: '',
  enabled: true,
};

const homeBlockOptions: Array<{ value: HomeOverviewBlockType; label: string }> = [
  { value: 'in_progress', label: 'Em andamento' },
  { value: 'alerts', label: 'Alertas' },
  { value: 'team_context', label: 'Equipe e contexto' },
  { value: 'recent', label: 'Recentes' },
  { value: 'quick_links', label: 'Acoes rapidas' },
];

function ProviderCatalogCard({
  card,
  canTest,
  testing,
  onTest,
}: {
  card: ProviderCard;
  canTest: boolean;
  testing: boolean;
  onTest: (code: string) => void;
}) {
  return (
    <article className="shell-panel p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-lg font-semibold text-[var(--text-primary)]">{card.provider.name}</p>
            <StatusBadge state={providerState(card)} />
          </div>
          <p className="mt-2 text-sm text-[var(--text-secondary)]">{card.provider.notes}</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
          <span>{humanizeToken(card.provider.category)}</span>
          <span>{humanizeToken(card.provider.apiStyle)}</span>
          <span>{providerImplementationStatus(card)}</span>
          <span>{providerEvidenceLevel(card)}</span>
          <span>{humanizeToken(card.provider.catalogState)}</span>
          <span>{providerStreamingMode(card)}</span>
          <span>{providerRuntimeMaturity(card)}</span>
        </div>
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-4">
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Credenciais</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{credentialSummary(card)}</p>
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Prontidao</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{providerReadiness(card)}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">
            {card.health?.message ?? 'Resumo operacional calculado sem consultar o provedor em tempo real.'}
          </p>
          {card.health ? (
            <p className="mt-2 text-xs text-[var(--text-tertiary)]">
              Fonte: {humanizeToken(card.health.healthSource)} | Snapshot: {humanizeToken(card.health.snapshotPersistence)}
            </p>
          ) : null}
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Compatibilidade</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.requiredHeaders.join(', ') || 'Nenhum requisito adicional.'}</p>
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Confianca</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">
            {providerImplementationStatus(card)} | {providerEvidenceLevel(card)}
          </p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">
            {humanizeToken(card.provider.businessPriority)} | {humanizeToken(card.provider.syncMode)}
          </p>
        </div>
      </div>

      <div className="mt-3 grid gap-3 xl:grid-cols-2">
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Custos</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.pricingSummary}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">{card.provider.rateLimitSummary}</p>
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso recomendado</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.routingModes.map(humanizeToken).join(', ')}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">Base de referencia: {humanizeToken(card.provider.documentationSource)}</p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <ExternalLink href={card.provider.apiKeyPortalUrl} label="Portal" />
        <ExternalLink href={card.provider.docsUrl} label="Docs" />
        {canTest ? (
          <button type="button" className="pill-button" onClick={() => onTest(card.provider.code)} disabled={testing}>
            <FiRefreshCcw size={14} />
            {testing ? 'Testando...' : 'Testar conexao'}
          </button>
        ) : null}
      </div>

      {card.connectivity ? (
        <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-sm font-semibold text-[var(--text-primary)]">Teste de conexao</p>
          <p className="mt-2 text-sm text-[var(--text-secondary)]">{card.connectivity.message}</p>
          {card.connectivity.latencyMs ? (
            <p className="mt-2 text-xs text-[var(--text-tertiary)]">Latencia: {card.connectivity.latencyMs} ms</p>
          ) : null}
        </div>
      ) : null}
    </article>
  );
}

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { session, refreshSummary } = useShell();
  const { preferences, preferencesError, updatePreferences } = useTheme();
  const [overview, setOverview] = useState<SettingsOverviewDto | null>(null);
  const [providers, setProviders] = useState<ProviderDto[]>([]);
  const [statuses, setStatuses] = useState<ProviderStatusDto[]>([]);
  const [health, setHealth] = useState<ProviderHealthDto[]>([]);
  const [credentials, setCredentials] = useState<ProviderCredentialDto[]>([]);
  const [connectivity, setConnectivity] = useState<Record<string, ProviderConnectivityDto>>({});
  const [budget, setBudget] = useState<BudgetSummaryDto | null>(null);
  const [budgetLoading, setBudgetLoading] = useState(false);
  const [budgetError, setBudgetError] = useState<string | null>(null);
  const [budgetSaving, setBudgetSaving] = useState(false);
  const [budgetDraft, setBudgetDraft] = useState({
    costCenter: '',
    chargebackMode: 'showback' as ChargebackMode,
    softLimitCredits: '300',
    hardLimitCredits: '450',
  });
  const [knowledgeSources, setKnowledgeSources] = useState<KnowledgeSourceDto[]>([]);
  const [knowledgeProjects, setKnowledgeProjects] = useState<ProjectDto[]>([]);
  const [knowledgeLoading, setKnowledgeLoading] = useState(false);
  const [knowledgeError, setKnowledgeError] = useState<string | null>(null);
  const [knowledgeSaving, setKnowledgeSaving] = useState(false);
  const [editingKnowledgeId, setEditingKnowledgeId] = useState<string | null>(null);
  const [knowledgeDraft, setKnowledgeDraft] = useState<CreateKnowledgeSourceRequest>(emptyKnowledgeDraft);
  const [shellCatalog, setShellCatalog] = useState<ShellCatalogDto | null>(null);
  const [shellCatalogLoading, setShellCatalogLoading] = useState(false);
  const [shellCatalogError, setShellCatalogError] = useState<string | null>(null);
  const [savingNavigationItemId, setSavingNavigationItemId] = useState<string | null>(null);
  const [savingTaskTypeId, setSavingTaskTypeId] = useState<string | null>(null);
  const [creatingNavigationItem, setCreatingNavigationItem] = useState(false);
  const [creatingTaskType, setCreatingTaskType] = useState(false);
  const [navigationItemDraft, setNavigationItemDraft] = useState<CreateShellCatalogItemRequest>(emptyShellCatalogItemDraft);
  const [taskTypeDraft, setTaskTypeDraft] = useState<CreateShellTaskTypeRequest>(emptyShellTaskTypeDraft);
  const [homeCatalog, setHomeCatalog] = useState<HomeOverviewCatalogDto | null>(null);
  const [homeCatalogLoading, setHomeCatalogLoading] = useState(false);
  const [homeCatalogError, setHomeCatalogError] = useState<string | null>(null);
  const [homeSettingsDraft, setHomeSettingsDraft] = useState<UpdateHomeOverviewSettingsRequest>(emptyHomeSettingsDraft);
  const [homeBlockDraft, setHomeBlockDraft] = useState<CreateHomeOverviewBlockRequest>(emptyHomeBlockDraft);
  const [savingHomeSettings, setSavingHomeSettings] = useState(false);
  const [creatingHomeBlock, setCreatingHomeBlock] = useState(false);
  const [savingHomeBlockId, setSavingHomeBlockId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [providerLoading, setProviderLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [providerError, setProviderError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [testingProviderCode, setTestingProviderCode] = useState<string | null>(null);
  const selectedSection = searchParams.get('section') ?? 'configuracoes';

  const permissions = session?.role.permissions ?? [];
  const canReadProviders = permissions.includes('providers.read');
  const canTestProviders = permissions.includes('providers.test');
  const canReadThreatIntel = permissions.includes('threat_intel.read');
  const canReadKnowledge = permissions.includes('knowledge.read');
  const canManageKnowledge = permissions.includes('knowledge.manage');
  const canReadBudgets = permissions.includes('budgets.read');
  const canManageBudgets = permissions.includes('budgets.manage');
  const canManageSettings = permissions.includes('settings.manage');

  useEffect(() => {
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        setOverview(await settingsService.getOverview());
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
        setOverview(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!canReadProviders) {
      setHealth([]);
      setProviders([]);
      setStatuses([]);
      setCredentials([]);
      setProviderLoading(false);
      setProviderError(null);
      return;
    }

    void (async () => {
      try {
        setProviderLoading(true);
        setProviderError(null);
        const [nextHealth, nextProviders, nextStatuses, nextCredentials] = await Promise.all([
          providerService.findProviderHealth(),
          providerService.findProviders(),
          providerService.findProviderStatuses(),
          providerService.findProviderCredentials(),
        ]);
        setHealth(nextHealth);
        setProviders(nextProviders);
        setStatuses(nextStatuses);
        setCredentials(nextCredentials);
      } catch (loadError) {
        setProviderError(toApiClientError(loadError).message);
      } finally {
        setProviderLoading(false);
      }
    })();
  }, [canReadProviders]);

  useEffect(() => {
    if (!canReadBudgets) {
      setBudget(null);
      setBudgetError(null);
      setBudgetLoading(false);
      return;
    }

    void (async () => {
      try {
        setBudgetLoading(true);
        setBudgetError(null);
        setBudget(await budgetService.getCurrent());
      } catch (loadError) {
        setBudget(null);
        setBudgetError(toApiClientError(loadError).message);
      } finally {
        setBudgetLoading(false);
      }
    })();
  }, [canReadBudgets]);

  useEffect(() => {
    if (!canReadKnowledge) {
      setKnowledgeSources([]);
      setKnowledgeProjects([]);
      setKnowledgeError(null);
      setKnowledgeLoading(false);
      return;
    }

    void (async () => {
      try {
        setKnowledgeLoading(true);
        setKnowledgeError(null);
        const [nextKnowledgeSources, nextProjects] = await Promise.all([
          knowledgeSourceService.findAll(),
          projectService.findAll(),
        ]);
        setKnowledgeSources(nextKnowledgeSources);
        setKnowledgeProjects(nextProjects);
      } catch (loadError) {
        setKnowledgeSources([]);
        setKnowledgeProjects([]);
        setKnowledgeError(toApiClientError(loadError).message);
      } finally {
        setKnowledgeLoading(false);
      }
    })();
  }, [canReadKnowledge]);

  useEffect(() => {
    if (!budget) {
      return;
    }
    setBudgetDraft({
      costCenter: budget.costCenter,
      chargebackMode: budget.chargebackMode,
      softLimitCredits: String(budget.softLimitCredits),
      hardLimitCredits: String(budget.hardLimitCredits),
    });
  }, [budget]);

  useEffect(() => {
    if (!canManageSettings) {
      setShellCatalog(null);
      setShellCatalogError(null);
      setShellCatalogLoading(false);
      return;
    }

    void (async () => {
      try {
        setShellCatalogLoading(true);
        setShellCatalogError(null);
        setShellCatalog(await shellCatalogService.getCatalog());
      } catch (loadError) {
        setShellCatalog(null);
        setShellCatalogError(toApiClientError(loadError).message);
      } finally {
        setShellCatalogLoading(false);
      }
    })();
  }, [canManageSettings]);

  useEffect(() => {
    if (!canManageSettings) {
      setHomeCatalog(null);
      setHomeCatalogError(null);
      setHomeCatalogLoading(false);
      return;
    }

    void (async () => {
      try {
        setHomeCatalogLoading(true);
        setHomeCatalogError(null);
        const catalog = await homeService.getCatalog();
        setHomeCatalog(catalog);
        setHomeSettingsDraft({
          headline: catalog.settings.headline,
          supportingText: catalog.settings.supportingText,
        });
      } catch (loadError) {
        setHomeCatalog(null);
        setHomeCatalogError(toApiClientError(loadError).message);
      } finally {
        setHomeCatalogLoading(false);
      }
    })();
  }, [canManageSettings]);

  const state = useMemo(() => {
    if (loading) return 'loading';
    if (error) return 'error';
    if (!overview) return 'empty';
    return 'live';
  }, [error, loading, overview]);

  const activeSection = overview?.sections.find((section) => section.key === selectedSection) ?? overview?.sections[0] ?? null;
  const cards = useMemo(() => providers.map((provider) => ({
    provider,
    status: statuses.find((item) => item.providerCode === provider.code),
    credentials: credentials.find((item) => item.providerCode === provider.code),
    health: health.find((item) => item.providerCode === provider.code),
    connectivity: connectivity[provider.code],
  })), [connectivity, credentials, health, providers, statuses]);

  const textRuntimeCards = cards.filter((card) => card.provider.category === 'text-runtime');
  const researchCards = cards.filter((card) => card.provider.category === 'research-search');
  const mediaCards = cards.filter((card) => card.provider.category === 'media-audio');
  const threatIntelCards = cards.filter((card) => card.provider.category === 'threat-intel');
  const knowledgeEnabledCount = knowledgeSources.filter((source) => source.enabledForAgents).length;
  const knowledgeProjectLinkedCount = knowledgeSources.filter((source) => source.projectId).length;

  const isConfigSection = activeSection?.key === 'configuracoes';
  const isKnowledgeSection = activeSection?.key === 'knowledge';
  const isFinopsSection = activeSection?.key === 'finops';
  const isProvidersSection = activeSection?.key === 'providers-runtime';
  const isHomeOverviewSection = activeSection?.key === 'home-overview';
  const isWorkspaceCatalogSection = activeSection?.key === 'workspace-catalog';
  const isThreatSection = activeSection?.key === 'threat-intelligence';
  const isLiveSection = activeSection?.availability === 'live';

  const handleThemeChange = async (nextTheme: ThemeMode) => {
    try {
      setSaving(true);
      await updatePreferences({ appearance: nextTheme });
      setOverview(await settingsService.getOverview());
    } finally {
      setSaving(false);
    }
  };

  const handleCommunicationToggle = async (key: 'emailUpdates' | 'productUpdates', checked: boolean) => {
    try {
      setSaving(true);
      await updatePreferences({ [key]: checked });
      setOverview(await settingsService.getOverview());
    } finally {
      setSaving(false);
    }
  };

  const handleConnectivityTest = async (providerCode: string) => {
    try {
      setTestingProviderCode(providerCode);
      const result = await providerService.testConnectivity(providerCode);
      setConnectivity((current) => ({ ...current, [providerCode]: result }));
    } catch (testError) {
        setConnectivity((current) => ({
          ...current,
          [providerCode]: {
            providerCode,
            providerName: providerCode,
            category: 'unknown',
            apiStyle: 'unknown',
            status: 'provider_error',
            configured: false,
            executionSupported: false,
            implementationStatus: 'catalog_only',
            evidenceLevel: 'offline_verified',
            streamingMode: 'unsupported',
            runtimeMaturity: 'catalog_only',
            latencyMs: null,
            message: toApiClientError(testError).message,
            missingCredentialEnvVars: [],
          },
        }));
    } finally {
      setTestingProviderCode(null);
    }
  };

  const handleBudgetSave = async () => {
    try {
      setBudgetSaving(true);
      setBudgetError(null);
      const updatedBudget = await budgetService.updateCurrent({
        costCenter: budgetDraft.costCenter.trim(),
        chargebackMode: budgetDraft.chargebackMode,
        softLimitCredits: Number.parseInt(budgetDraft.softLimitCredits, 10),
        hardLimitCredits: Number.parseInt(budgetDraft.hardLimitCredits, 10),
      });
      setBudget(updatedBudget);
      setOverview(await settingsService.getOverview());
    } catch (saveError) {
      setBudgetError(toApiClientError(saveError).message);
    } finally {
      setBudgetSaving(false);
    }
  };

  const resetKnowledgeDraft = () => {
    setEditingKnowledgeId(null);
    setKnowledgeDraft(emptyKnowledgeDraft);
  };

  const handleEditKnowledge = (source: KnowledgeSourceDto) => {
    setEditingKnowledgeId(source.id);
    setKnowledgeDraft({
      title: source.title,
      sourceType: source.sourceType,
      projectId: source.projectId ?? '',
      sourceUri: source.sourceUri ?? '',
      documentCount: source.documentCount,
      enabledForAgents: source.enabledForAgents,
      note: source.note,
      statusLabel: source.statusLabel,
      availability: source.availability,
    });
  };

  const handleKnowledgeSave = async () => {
    try {
      setKnowledgeSaving(true);
      setKnowledgeError(null);

      const payload: UpdateKnowledgeSourceRequest = {
        title: knowledgeDraft.title.trim(),
        sourceType: knowledgeDraft.sourceType?.trim(),
        projectId: knowledgeDraft.projectId?.trim() || undefined,
        sourceUri: knowledgeDraft.sourceUri?.trim() || undefined,
        documentCount: Number(knowledgeDraft.documentCount ?? 0),
        enabledForAgents: knowledgeDraft.enabledForAgents ?? true,
        statusLabel: knowledgeDraft.statusLabel?.trim() || undefined,
        availability: knowledgeDraft.availability,
        note: knowledgeDraft.note.trim(),
      };

      const nextKnowledgeSource = editingKnowledgeId
        ? await knowledgeSourceService.update(editingKnowledgeId, payload)
        : await knowledgeSourceService.create(payload as CreateKnowledgeSourceRequest);

      setKnowledgeSources((current) => {
        const withoutCurrent = current.filter((source) => source.id !== nextKnowledgeSource.id);
        return [nextKnowledgeSource, ...withoutCurrent];
      });
      resetKnowledgeDraft();
      setOverview(await settingsService.getOverview());
    } catch (saveError) {
      setKnowledgeError(toApiClientError(saveError).message);
    } finally {
      setKnowledgeSaving(false);
    }
  };

  const handleKnowledgeDelete = async (sourceId: string) => {
    try {
      setKnowledgeSaving(true);
      setKnowledgeError(null);
      await knowledgeSourceService.remove(sourceId);
      setKnowledgeSources((current) => current.filter((source) => source.id !== sourceId));
      if (editingKnowledgeId === sourceId) {
        resetKnowledgeDraft();
      }
      setOverview(await settingsService.getOverview());
    } catch (deleteError) {
      setKnowledgeError(toApiClientError(deleteError).message);
    } finally {
      setKnowledgeSaving(false);
    }
  };

  const updateShellCatalogItemDraft = (itemId: string, updater: (item: ShellCatalogItemDto) => ShellCatalogItemDto) => {
    setShellCatalog((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        items: current.items.map((item) => (item.id === itemId ? updater(item) : item)),
      };
    });
  };

  const updateShellTaskTypeDraft = (taskTypeId: string, updater: (item: ShellCatalogTaskTypeDto) => ShellCatalogTaskTypeDto) => {
    setShellCatalog((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        taskTypes: current.taskTypes.map((item) => (item.taskType === taskTypeId ? updater(item) : item)),
      };
    });
  };

  const handleNavigationItemSave = async (item: ShellCatalogItemDto) => {
    try {
      setSavingNavigationItemId(item.id);
      setShellCatalogError(null);
      const savedItem = await shellCatalogService.updateNavigationItem(item.id, {
        label: item.label,
        path: item.path,
        description: item.description,
        icon: item.icon,
        availability: item.availability,
        group: item.group,
        sortOrder: item.sortOrder,
        enabled: item.enabled,
        keywords: item.keywords,
      });
      setShellCatalog((current) => {
        if (!current) {
          return current;
        }
        return {
          ...current,
          items: current.items
            .map((catalogItem) => (catalogItem.id === savedItem.id ? savedItem : catalogItem))
            .sort((left, right) => left.sortOrder - right.sortOrder),
        };
      });
      await refreshSummary();
    } catch (saveError) {
      setShellCatalogError(toApiClientError(saveError).message);
    } finally {
      setSavingNavigationItemId(null);
    }
  };

  const handleTaskTypeSave = async (item: ShellCatalogTaskTypeDto) => {
    try {
      setSavingTaskTypeId(item.taskType);
      setShellCatalogError(null);
      const savedItem = await shellCatalogService.updateTaskType(item.taskType, {
        label: item.label,
        description: item.description,
        sortOrder: item.sortOrder,
        enabled: item.enabled,
      });
      setShellCatalog((current) => {
        if (!current) {
          return current;
        }
        return {
          ...current,
          taskTypes: current.taskTypes
            .map((catalogItem) => (catalogItem.taskType === savedItem.taskType ? savedItem : catalogItem))
            .sort((left, right) => left.sortOrder - right.sortOrder),
        };
      });
      await refreshSummary();
    } catch (saveError) {
      setShellCatalogError(toApiClientError(saveError).message);
    } finally {
      setSavingTaskTypeId(null);
    }
  };

  const handleNavigationItemCreate = async () => {
    try {
      setCreatingNavigationItem(true);
      setShellCatalogError(null);
      const savedItem = await shellCatalogService.createNavigationItem({
        ...navigationItemDraft,
        keywords: navigationItemDraft.keywords?.filter(Boolean) ?? [],
      });
      setShellCatalog((current) => current ? {
        ...current,
        items: [...current.items, savedItem].sort((left, right) => left.sortOrder - right.sortOrder),
      } : current);
      setNavigationItemDraft(emptyShellCatalogItemDraft);
      await refreshSummary();
    } catch (saveError) {
      setShellCatalogError(toApiClientError(saveError).message);
    } finally {
      setCreatingNavigationItem(false);
    }
  };

  const handleNavigationItemDelete = async (item: ShellCatalogItemDto) => {
    if (!window.confirm(`Remover a area "${item.label}" da shell?`)) {
      return;
    }
    try {
      setSavingNavigationItemId(item.id);
      setShellCatalogError(null);
      await shellCatalogService.deleteNavigationItem(item.id);
      setShellCatalog((current) => current ? {
        ...current,
        items: current.items.filter((catalogItem) => catalogItem.id !== item.id),
      } : current);
      await refreshSummary();
    } catch (deleteError) {
      setShellCatalogError(toApiClientError(deleteError).message);
    } finally {
      setSavingNavigationItemId(null);
    }
  };

  const handleTaskTypeCreate = async () => {
    try {
      setCreatingTaskType(true);
      setShellCatalogError(null);
      const savedItem = await shellCatalogService.createTaskType(taskTypeDraft);
      setShellCatalog((current) => current ? {
        ...current,
        taskTypes: [...current.taskTypes, savedItem].sort((left, right) => left.sortOrder - right.sortOrder),
      } : current);
      setTaskTypeDraft(emptyShellTaskTypeDraft);
      await refreshSummary();
    } catch (saveError) {
      setShellCatalogError(toApiClientError(saveError).message);
    } finally {
      setCreatingTaskType(false);
    }
  };

  const handleTaskTypeDelete = async (item: ShellCatalogTaskTypeDto) => {
    if (!window.confirm(`Remover o tipo de tarefa "${item.label}"?`)) {
      return;
    }
    try {
      setSavingTaskTypeId(item.taskType);
      setShellCatalogError(null);
      await shellCatalogService.deleteTaskType(item.taskType);
      setShellCatalog((current) => current ? {
        ...current,
        taskTypes: current.taskTypes.filter((catalogItem) => catalogItem.taskType !== item.taskType),
      } : current);
      await refreshSummary();
    } catch (deleteError) {
      setShellCatalogError(toApiClientError(deleteError).message);
    } finally {
      setSavingTaskTypeId(null);
    }
  };

  const updateHomeBlockDraftItem = (blockId: string, updater: (item: HomeOverviewBlockDto) => HomeOverviewBlockDto) => {
    setHomeCatalog((current) => {
      if (!current) {
        return current;
      }
      return {
        ...current,
        blocks: current.blocks.map((item) => (item.id === blockId ? updater(item) : item)),
      };
    });
  };

  const handleHomeSettingsSave = async () => {
    try {
      setSavingHomeSettings(true);
      setHomeCatalogError(null);
      const savedSettings = await homeService.updateSettings({
        headline: homeSettingsDraft.headline?.trim(),
        supportingText: homeSettingsDraft.supportingText?.trim(),
      });
      setHomeCatalog((current) => current ? { ...current, settings: savedSettings } : current);
      setHomeSettingsDraft(savedSettings);
      await refreshSummary();
      setOverview(await settingsService.getOverview());
    } catch (saveError) {
      setHomeCatalogError(toApiClientError(saveError).message);
    } finally {
      setSavingHomeSettings(false);
    }
  };

  const handleHomeBlockCreate = async () => {
    try {
      setCreatingHomeBlock(true);
      setHomeCatalogError(null);
      const savedBlock = await homeService.createBlock({
        ...homeBlockDraft,
        id: homeBlockDraft.id.trim(),
        title: homeBlockDraft.title.trim(),
        description: homeBlockDraft.description.trim(),
        ctaLabel: homeBlockDraft.ctaLabel?.trim() || '',
        ctaPath: homeBlockDraft.ctaPath?.trim() || '',
      });
      setHomeCatalog((current) => current ? {
        ...current,
        blocks: [...current.blocks, savedBlock].sort((left, right) => left.sortOrder - right.sortOrder),
      } : current);
      setHomeBlockDraft(emptyHomeBlockDraft);
      await refreshSummary();
    } catch (saveError) {
      setHomeCatalogError(toApiClientError(saveError).message);
    } finally {
      setCreatingHomeBlock(false);
    }
  };

  const handleHomeBlockSave = async (block: HomeOverviewBlockDto) => {
    try {
      setSavingHomeBlockId(block.id);
      setHomeCatalogError(null);
      const savedBlock = await homeService.updateBlock(block.id, {
        title: block.title,
        description: block.description,
        sortOrder: block.sortOrder,
        maxItems: block.maxItems,
        ctaLabel: block.ctaLabel?.trim() || '',
        ctaPath: block.ctaPath?.trim() || '',
        enabled: block.enabled,
      });
      setHomeCatalog((current) => current ? {
        ...current,
        blocks: current.blocks
          .map((item) => (item.id === savedBlock.id ? savedBlock : item))
          .sort((left, right) => left.sortOrder - right.sortOrder),
      } : current);
      await refreshSummary();
    } catch (saveError) {
      setHomeCatalogError(toApiClientError(saveError).message);
    } finally {
      setSavingHomeBlockId(null);
    }
  };

  const handleHomeBlockDelete = async (block: HomeOverviewBlockDto) => {
    if (!window.confirm(`Remover o bloco "${block.title}" da tela inicial?`)) {
      return;
    }
    try {
      setSavingHomeBlockId(block.id);
      setHomeCatalogError(null);
      await homeService.deleteBlock(block.id);
      setHomeCatalog((current) => current ? {
        ...current,
        blocks: current.blocks.filter((item) => item.id !== block.id),
      } : current);
      await refreshSummary();
    } catch (deleteError) {
      setHomeCatalogError(toApiClientError(deleteError).message);
    } finally {
      setSavingHomeBlockId(null);
    }
  };

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Configuracoes do workspace"
        description="Ajuste preferencias, limites, conhecimento e acesso aos provedores do workspace."
        state="live"
        detail={preferencesError ?? 'As informacoes desta tela refletem o estado atual do workspace.'}
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando configuracoes..."
        errorTitle="Nao foi possivel carregar as configuracoes."
        errorDescription="Tente novamente em instantes."
        errorDetail={error ?? undefined}
        onRetry={() => window.location.reload()}
        emptyTitle="Nenhuma configuracao encontrada."
        emptyDescription="O workspace ainda nao devolveu o overview de configuracoes."
      >
        {overview && activeSection ? (
          <section className="modal-page overflow-hidden">
            <div className="grid min-h-[720px] gap-0 xl:grid-cols-[320px_minmax(0,1fr)]">
              <aside className="border-r p-5" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--background-menu-white)' }}>
                <div className="flex items-center gap-3 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                  <div className="flex h-11 w-11 items-center justify-center rounded-full border" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--accent)' }}>
                    <FiSettings size={18} />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-[var(--text-primary)]">{overview.workspaceName}</p>
                    <p className="text-sm text-[var(--text-secondary)]">{overview.organizationName}</p>
                  </div>
                </div>

                <div className="mt-6 space-y-2">
                  {overview.sections.map((section) => (
                    <button
                      key={section.key}
                      type="button"
                      className="flex w-full items-center justify-between rounded-[12px] border px-4 py-4 text-left transition-all duration-200"
                      style={{
                        borderColor: activeSection.key === section.key ? 'var(--surface-border-strong)' : 'var(--surface-border-main)',
                        background: activeSection.key === section.key ? 'var(--fill-tsp-white-dark)' : 'var(--fill-tsp-white-main)',
                      }}
                      onClick={() => setSearchParams({ section: section.key })}
                    >
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <p className="text-sm font-semibold text-[var(--text-primary)]">{section.title}</p>
                          <StatusBadge state={section.availability} />
                        </div>
                        <p className="mt-1 text-sm text-[var(--text-secondary)]">{section.description}</p>
                      </div>
                      <FiChevronRight size={16} className="shrink-0 text-[var(--text-tertiary)]" />
                    </button>
                  ))}
                </div>
              </aside>

              <div className="space-y-4 p-6 sm:p-7" style={{ background: 'var(--surface-bg)' }}>
                <section className="manus-banner">
                  <div className="flex flex-wrap items-center gap-3">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">{activeSection.title}</p>
                    <StatusBadge state={activeSection.availability} />
                  </div>
                  <h1 className="mt-3 text-3xl font-semibold text-[var(--text-primary)]">{activeSection.title}</h1>
                  <p className="mt-3 text-sm leading-7 text-[var(--text-secondary)]">{activeSection.description}</p>
                </section>

                {isConfigSection ? (
                  <section className="grid gap-4 lg:grid-cols-2">
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Aparencia</p>
                      <div className="mt-4 grid gap-3">
                        {([
                          { value: 'light', label: 'Light', icon: FiSun },
                          { value: 'dark', label: 'Dark', icon: FiMoon },
                        ] as const).map((option) => {
                          const Icon = option.icon;
                          const selected = preferences.appearance === option.value;
                          return (
                            <button
                              key={option.value}
                              type="button"
                              className="flex items-center justify-between rounded-[12px] border px-4 py-4 text-left transition-all duration-200"
                              style={{
                                borderColor: selected ? 'var(--surface-border-strong)' : 'var(--surface-border-main)',
                                background: selected ? 'var(--fill-tsp-white-dark)' : 'var(--fill-tsp-white-main)',
                              }}
                              onClick={() => void handleThemeChange(option.value)}
                              disabled={saving}
                            >
                              <span className="flex items-center gap-3">
                                <Icon size={16} />
                                {option.label}
                              </span>
                              {selected ? <FiCheckCircle size={16} className="text-[var(--accent)]" /> : null}
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Comunicacao</p>
                      <div className="mt-4 space-y-3">
                        {([
                          ['emailUpdates', 'Atualizacoes por e-mail', 'Mudancas relevantes do produto.'],
                          ['productUpdates', 'Atualizacoes de produto', 'Novos recursos e disponibilidade.'],
                        ] as const).map(([key, title, description]) => (
                          <label key={key} className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                            <span>
                              <p className="text-sm font-semibold text-[var(--text-primary)]">{title}</p>
                              <p className="text-sm text-[var(--text-secondary)]">{description}</p>
                            </span>
                            <input
                              type="checkbox"
                              checked={preferences[key]}
                              onChange={(event) => void handleCommunicationToggle(key, event.target.checked)}
                              disabled={saving}
                            />
                          </label>
                        ))}
                      </div>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Idioma</p>
                      <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                        <div className="flex items-center gap-3">
                          <FiMail size={16} className="text-[var(--text-tertiary)]" />
                          <div>
                            <p className="text-sm font-semibold text-[var(--text-primary)]">{preferences.languageCode}</p>
                            <p className="text-sm text-[var(--text-secondary)]">Idioma salvo para este usuario.</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Workspace</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.workspaceName}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">Acesso atual: {overview.roleLabel}</p>
                    </div>
                  </section>
                ) : null}

                {isKnowledgeSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-4">
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Fontes</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{knowledgeSources.length}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Disponiveis para tarefas</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{knowledgeEnabledCount}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Ligadas a projetos</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{knowledgeProjectLinkedCount}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Workspace</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.knowledgeSources}</p>
                      </div>
                    </div>

                    {knowledgeLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando fontes de conhecimento...</div> : null}
                    {knowledgeError ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">{knowledgeError}</div> : null}

                    {!knowledgeLoading ? (
                      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
                        <article className="shell-panel p-5">
                          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Catalogo do workspace</p>
                          <div className="mt-4 space-y-3">
                            {knowledgeSources.length === 0 ? (
                              <div className="rounded-[12px] border px-4 py-5 text-sm text-[var(--text-secondary)]" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                                Nenhuma fonte registrada ainda.
                              </div>
                            ) : (
                              knowledgeSources.map((source) => (
                                <div key={source.id} className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                                  <div className="flex flex-wrap items-start justify-between gap-3">
                                    <div className="min-w-0">
                                      <div className="flex flex-wrap items-center gap-2">
                                        <p className="text-sm font-semibold text-[var(--text-primary)]">{source.title}</p>
                                        <StatusBadge state={source.availability} />
                                      </div>
                                      <p className="mt-1 text-sm text-[var(--text-secondary)]">
                                        {source.sourceType} {source.projectName ? `. ${source.projectName}` : '. Sem projeto'}
                                      </p>
                                    </div>
                                    {canManageKnowledge ? (
                                      <div className="flex gap-2">
                                        <button type="button" className="pill-button" onClick={() => handleEditKnowledge(source)} disabled={knowledgeSaving}>
                                          <FiEdit3 size={14} />
                                          Editar
                                        </button>
                                        <button type="button" className="pill-button" onClick={() => void handleKnowledgeDelete(source.id)} disabled={knowledgeSaving}>
                                          <FiTrash2 size={14} />
                                          Remover
                                        </button>
                                      </div>
                                    ) : null}
                                  </div>
                                  <div className="mt-4 grid gap-3 lg:grid-cols-4">
                                    <div>
                                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Documentos</p>
                                      <p className="mt-2 text-sm text-[var(--text-primary)]">{source.documentCount}</p>
                                    </div>
                                    <div>
                                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso em tarefas</p>
                                      <p className="mt-2 text-sm text-[var(--text-primary)]">{source.enabledForAgents ? 'Ativo' : 'Desligado'}</p>
                                    </div>
                                    <div>
                                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Status</p>
                                      <p className="mt-2 text-sm text-[var(--text-primary)]">{source.statusLabel}</p>
                                    </div>
                                    <div>
                                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Ultima indexacao</p>
                                      <p className="mt-2 text-sm text-[var(--text-primary)]">{source.lastIndexedAt ?? source.updatedAt}</p>
                                    </div>
                                  </div>
                                  <p className="mt-4 text-sm leading-6 text-[var(--text-secondary)]">{source.note}</p>
                                  {source.sourceUri ? (
                                    <p className="mt-3 text-xs text-[var(--text-tertiary)]">
                                      URI: <span className="font-semibold text-[var(--text-secondary)]">{source.sourceUri}</span>
                                    </p>
                                  ) : null}
                                </div>
                              ))
                            )}
                          </div>
                        </article>

                        <article className="shell-panel p-5">
                          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">
                            {editingKnowledgeId ? 'Editar fonte' : 'Nova fonte'}
                          </p>
                          <div className="mt-4 space-y-3">
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Titulo</span>
                              <input
                                aria-label="Titulo da fonte de conhecimento"
                                value={knowledgeDraft.title}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, title: event.target.value }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Tipo</span>
                              <select
                                aria-label="Tipo da fonte de conhecimento"
                                value={knowledgeDraft.sourceType}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, sourceType: event.target.value }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              >
                                {['library', 'repository', 'document-store', 'agent-thread', 'search-grounding', 'upload'].map((sourceType) => (
                                  <option key={sourceType} value={sourceType}>{sourceType}</option>
                                ))}
                              </select>
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Projeto</span>
                              <select
                                aria-label="Projeto da fonte de conhecimento"
                                value={knowledgeDraft.projectId ?? ''}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, projectId: event.target.value }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              >
                                <option value="">Sem projeto</option>
                                {knowledgeProjects.map((project) => (
                                  <option key={project.id} value={project.id}>{project.name}</option>
                                ))}
                              </select>
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">URI de origem</span>
                              <input
                                aria-label="URI da fonte de conhecimento"
                                value={knowledgeDraft.sourceUri ?? ''}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, sourceUri: event.target.value }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Documentos indexados</span>
                              <input
                                aria-label="Documentos indexados da fonte de conhecimento"
                                type="number"
                                min={0}
                                value={knowledgeDraft.documentCount ?? 0}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, documentCount: Number.parseInt(event.target.value || '0', 10) }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                              <span>
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Disponivel para tarefas</p>
                                <p className="text-sm text-[var(--text-secondary)]">Permite usar esta fonte como contexto nas tarefas e conversas.</p>
                              </span>
                              <input
                                aria-label="Disponivel para tarefas"
                                type="checkbox"
                                checked={knowledgeDraft.enabledForAgents ?? true}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, enabledForAgents: event.target.checked }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Nota operacional</span>
                              <textarea
                                aria-label="Nota operacional da fonte de conhecimento"
                                rows={4}
                                value={knowledgeDraft.note}
                                onChange={(event) => setKnowledgeDraft((current) => ({ ...current, note: event.target.value }))}
                                disabled={!canManageKnowledge || knowledgeSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <div className="flex flex-wrap gap-3">
                              <button type="button" className="pill-button" onClick={() => void handleKnowledgeSave()} disabled={!canManageKnowledge || knowledgeSaving}>
                                {knowledgeSaving ? 'Salvando...' : editingKnowledgeId ? 'Atualizar fonte' : 'Criar fonte'}
                              </button>
                              {editingKnowledgeId ? (
                                <button type="button" className="pill-button" onClick={resetKnowledgeDraft} disabled={knowledgeSaving}>
                                  Cancelar
                                </button>
                              ) : null}
                            </div>
                            {!canManageKnowledge ? (
                              <p className="text-xs text-[var(--text-tertiary)]">
                                Seu perfil ve as fontes do workspace, mas nao pode alterar o catalogo de conhecimento.
                              </p>
                            ) : null}
                          </div>
                        </article>
                      </div>
                    ) : null}

                    <section className="manus-banner">
                      <div className="flex items-start gap-3">
                        <FiDatabase size={18} className="mt-0.5 text-[var(--accent)]" />
                        <div>
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Base de conhecimento do workspace</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
                            Registre fontes por projeto, acompanhe atualizacoes e controle o uso delas nas tarefas.
                          </p>
                        </div>
                      </div>
                    </section>
                  </section>
                ) : null}

                {isFinopsSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-4">
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Centro de custo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget?.costCenter ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Modo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{humanizeChargebackMode(budget?.chargebackMode)}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Limite de alerta</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget ? `${budget.consumedCredits}/${budget.softLimitCredits}` : '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Limite maximo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget ? `${budget.consumedCredits}/${budget.hardLimitCredits}` : '--'}</p>
                      </div>
                    </div>

                    {budgetLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando uso do workspace...</div> : null}
                    {budgetError ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">{budgetError}</div> : null}

                    {budget ? (
                      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)]">
                        <article className="shell-panel p-5">
                          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Resumo operacional</p>
                          <div className="mt-4 grid gap-3 lg:grid-cols-2">
                            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Status</p>
                              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.budgetStatus.replace(/_/g, ' ')}</p>
                              <p className="mt-2 text-xs text-[var(--text-secondary)]">{budget.note}</p>
                            </div>
                            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Rateio do consumo</p>
                              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{humanizeChargebackMode(budget.chargebackMode)}</p>
                              <p className="mt-2 text-xs text-[var(--text-secondary)]">Define como o consumo sera acompanhado e repassado.</p>
                            </div>
                            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Utilizacao soft</p>
                              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.softLimitUtilizationPercent}%</p>
                              <p className="mt-2 text-xs text-[var(--text-secondary)]">Restantes: {budget.remainingSoftCredits}</p>
                            </div>
                            <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Utilizacao hard</p>
                              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.hardLimitUtilizationPercent}%</p>
                              <p className="mt-2 text-xs text-[var(--text-secondary)]">Restantes: {budget.remainingHardCredits}</p>
                            </div>
                          </div>
                        </article>

                        <article className="shell-panel p-5">
                          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Guardrails do workspace</p>
                          <div className="mt-4 space-y-3">
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Centro de custo</span>
                              <input
                                value={budgetDraft.costCenter}
                                onChange={(event) => setBudgetDraft((current) => ({ ...current, costCenter: event.target.value }))}
                                disabled={!canManageBudgets || budgetSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Modo de alocacao</span>
                              <select
                                aria-label="Modo de alocacao do workspace"
                                value={budgetDraft.chargebackMode}
                                onChange={(event) => setBudgetDraft((current) => ({ ...current, chargebackMode: event.target.value as ChargebackMode }))}
                                disabled={!canManageBudgets || budgetSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              >
                                <option value="showback">showback</option>
                                <option value="chargeback">chargeback</option>
                              </select>
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Limite de alerta</span>
                              <input
                                aria-label="Limite de alerta do workspace"
                                type="number"
                                min={0}
                                value={budgetDraft.softLimitCredits}
                                onChange={(event) => setBudgetDraft((current) => ({ ...current, softLimitCredits: event.target.value }))}
                                disabled={!canManageBudgets || budgetSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Limite maximo</span>
                              <input
                                aria-label="Limite maximo do workspace"
                                type="number"
                                min={0}
                                value={budgetDraft.hardLimitCredits}
                                onChange={(event) => setBudgetDraft((current) => ({ ...current, hardLimitCredits: event.target.value }))}
                                disabled={!canManageBudgets || budgetSaving}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <button type="button" className="pill-button" onClick={() => void handleBudgetSave()} disabled={!canManageBudgets || budgetSaving}>
                              {budgetSaving ? 'Salvando...' : 'Salvar limites'}
                            </button>
                            {!canManageBudgets ? <p className="text-xs text-[var(--text-tertiary)]">Seu perfil ve o budget, mas nao pode alterar os guardrails do workspace.</p> : null}
                          </div>
                        </article>
                      </div>
                    ) : null}
                  </section>
                ) : null}

                {isProvidersSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-3">
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Provedores principais</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{textRuntimeCards.length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Configurados</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{statuses.filter((item) => item.configured).length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Categorias</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">4</p></div>
                    </div>

                    {providerLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando provedores...</div> : null}
                    {providerError ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">{providerError}</div> : null}

                    {!providerLoading && !providerError ? (
                      <div className="grid gap-4 xl:grid-cols-2">
                        {[...textRuntimeCards, ...researchCards, ...mediaCards].map((card) => (
                          <ProviderCatalogCard
                            key={card.provider.code}
                            card={card}
                            canTest={canTestProviders}
                            testing={testingProviderCode === card.provider.code}
                            onTest={handleConnectivityTest}
                          />
                        ))}
                      </div>
                    ) : null}
                  </section>
                ) : null}

                {isHomeOverviewSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-4">
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Blocos ativos</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{homeCatalog?.blocks.filter((block) => block.enabled).length ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Mensagem principal</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Editavel</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Reflexo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Imediato</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Origem</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Banco</p>
                      </div>
                    </div>

                    {homeCatalogLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando tela inicial...</div> : null}
                    {homeCatalogError ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">{homeCatalogError}</div> : null}

                    {homeCatalog ? (
                      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
                        <article className="shell-panel p-5">
                          <div className="flex items-center justify-between gap-3">
                            <div>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Mensagem principal</p>
                              <p className="mt-2 text-sm text-[var(--text-secondary)]">Defina o titulo e o texto de apoio exibidos no inicio do workspace.</p>
                            </div>
                            <button
                              type="button"
                              className="pill-button"
                              onClick={() => void handleHomeSettingsSave()}
                              disabled={!canManageSettings || savingHomeSettings}
                            >
                              {savingHomeSettings ? 'Salvando...' : 'Salvar mensagem'}
                            </button>
                          </div>

                          <div className="mt-4 grid gap-3">
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Titulo principal</span>
                              <input
                                aria-label="Titulo principal"
                                value={homeSettingsDraft.headline ?? ''}
                                onChange={(event) => setHomeSettingsDraft((current) => ({ ...current, headline: event.target.value }))}
                                disabled={!canManageSettings || savingHomeSettings}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                              />
                            </label>
                            <label className="grid gap-2">
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Texto de apoio</span>
                              <textarea
                                aria-label="Texto de apoio"
                                value={homeSettingsDraft.supportingText ?? ''}
                                onChange={(event) => setHomeSettingsDraft((current) => ({ ...current, supportingText: event.target.value }))}
                                disabled={!canManageSettings || savingHomeSettings}
                                rows={4}
                                className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                              />
                            </label>
                          </div>
                        </article>

                        <article className="shell-panel p-5">
                          <div className="flex items-center justify-between gap-3">
                            <div>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Blocos do painel</p>
                              <p className="mt-2 text-sm text-[var(--text-secondary)]">Controle a ordem, o texto e a visibilidade dos blocos da tela inicial.</p>
                            </div>
                            <button
                              type="button"
                              className="pill-button"
                              onClick={() => void handleHomeBlockCreate()}
                              disabled={!canManageSettings || creatingHomeBlock}
                            >
                              {creatingHomeBlock ? 'Criando...' : 'Criar bloco'}
                            </button>
                          </div>

                          <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                            <div className="grid gap-3 lg:grid-cols-2">
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">ID</span>
                                <input
                                  aria-label="ID do novo bloco"
                                  value={homeBlockDraft.id}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, id: event.target.value }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Tipo do bloco</span>
                                <select
                                  aria-label="Tipo do bloco"
                                  value={homeBlockDraft.blockType}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, blockType: event.target.value as HomeOverviewBlockType }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                >
                                  {homeBlockOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                      {option.label}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Titulo do bloco</span>
                                <input
                                  aria-label="Titulo do novo bloco"
                                  value={homeBlockDraft.title}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, title: event.target.value }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Ordem</span>
                                <input
                                  aria-label="Ordem do novo bloco"
                                  type="number"
                                  min={0}
                                  value={homeBlockDraft.sortOrder}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, sortOrder: Number.parseInt(event.target.value || '0', 10) }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Itens exibidos</span>
                                <input
                                  aria-label="Limite do novo bloco"
                                  type="number"
                                  min={1}
                                  max={12}
                                  value={homeBlockDraft.maxItems ?? 4}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, maxItems: Number.parseInt(event.target.value || '4', 10) }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2 lg:col-span-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao do bloco</span>
                                <input
                                  aria-label="Descricao do novo bloco"
                                  value={homeBlockDraft.description}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, description: event.target.value }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Texto do CTA</span>
                                <input
                                  aria-label="Texto do CTA do novo bloco"
                                  value={homeBlockDraft.ctaLabel ?? ''}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, ctaLabel: event.target.value }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Rota do CTA</span>
                                <input
                                  aria-label="Rota do CTA do novo bloco"
                                  value={homeBlockDraft.ctaPath ?? ''}
                                  onChange={(event) => setHomeBlockDraft((current) => ({ ...current, ctaPath: event.target.value }))}
                                  disabled={!canManageSettings || creatingHomeBlock}
                                  placeholder="/tasks"
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                            </div>
                          </div>

                          <div className="mt-4 space-y-3">
                            {homeCatalog.blocks.map((block) => (
                              <div
                                key={block.id}
                                data-testid={`home-block-${block.id}`}
                                className="rounded-[12px] border px-4 py-4"
                                style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}
                              >
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                  <div>
                                    <div className="flex flex-wrap items-center gap-2">
                                      <p className="text-sm font-semibold text-[var(--text-primary)]">{block.id}</p>
                                      <StatusBadge state={block.enabled ? 'live' : 'restricted'} />
                                    </div>
                                    <p className="mt-1 text-xs text-[var(--text-tertiary)]">{homeBlockOptions.find((option) => option.value === block.blockType)?.label ?? block.blockType}</p>
                                  </div>
                                  <button
                                    type="button"
                                    className="pill-button"
                                    aria-label={`Salvar bloco ${block.id}`}
                                    onClick={() => void handleHomeBlockSave(block)}
                                    disabled={!canManageSettings || savingHomeBlockId === block.id}
                                  >
                                    {savingHomeBlockId === block.id ? 'Salvando...' : 'Salvar bloco'}
                                  </button>
                                </div>

                                <div className="mt-4 grid gap-3">
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Titulo</span>
                                    <input
                                      aria-label={`Titulo do bloco ${block.id}`}
                                      value={block.title}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, title: event.target.value }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao</span>
                                    <input
                                      aria-label={`Descricao do bloco ${block.id}`}
                                      value={block.description}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, description: event.target.value }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Ordem</span>
                                    <input
                                      aria-label={`Ordem do bloco ${block.id}`}
                                      type="number"
                                      min={0}
                                      value={block.sortOrder}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, sortOrder: Number.parseInt(event.target.value || '0', 10) }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Itens exibidos</span>
                                    <input
                                      aria-label={`Limite do bloco ${block.id}`}
                                      type="number"
                                      min={1}
                                      max={12}
                                      value={block.maxItems ?? 4}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, maxItems: Number.parseInt(event.target.value || '4', 10) }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Texto do CTA</span>
                                    <input
                                      aria-label={`Texto do CTA do bloco ${block.id}`}
                                      value={block.ctaLabel ?? ''}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, ctaLabel: event.target.value }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Rota do CTA</span>
                                    <input
                                      aria-label={`Rota do CTA do bloco ${block.id}`}
                                      value={block.ctaPath ?? ''}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, ctaPath: event.target.value }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                      placeholder="/tasks"
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                                    <span>
                                      <p className="text-sm font-semibold text-[var(--text-primary)]">Exibir no inicio</p>
                                      <p className="text-sm text-[var(--text-secondary)]">Quando desligado, esse bloco sai da tela inicial do workspace.</p>
                                    </span>
                                    <input
                                      type="checkbox"
                                      aria-label={`Exibir bloco ${block.id} no inicio`}
                                      checked={block.enabled}
                                      onChange={(event) => updateHomeBlockDraftItem(block.id, (current) => ({ ...current, enabled: event.target.checked }))}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                    />
                                  </label>
                                  <div className="flex justify-end">
                                    <button
                                      type="button"
                                      className="pill-button"
                                      aria-label={`Remover bloco ${block.id}`}
                                      onClick={() => void handleHomeBlockDelete(block)}
                                      disabled={!canManageSettings || savingHomeBlockId === block.id}
                                    >
                                      <FiTrash2 size={14} />
                                      Remover bloco
                                    </button>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </article>
                      </div>
                    ) : null}
                  </section>
                ) : null}

                {isWorkspaceCatalogSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-4">
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Areas ativas</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{shellCatalog?.items.filter((item) => item.enabled).length ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Tipos ativos</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{shellCatalog?.taskTypes.filter((item) => item.enabled).length ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Navegacao</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Gerenciavel</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Reflexo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Imediato</p>
                      </div>
                    </div>

                    {shellCatalogLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando menu e tipos de tarefa...</div> : null}
                    {shellCatalogError ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">{shellCatalogError}</div> : null}

                    {shellCatalog ? (
                      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
                        <article className="shell-panel p-5">
                          <div className="flex items-center justify-between gap-3">
                            <div>
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Menu principal</p>
                              <p className="mt-2 text-sm text-[var(--text-secondary)]">Ajuste nome, rota, icone, ordem, grupo e disponibilidade das areas exibidas no workspace.</p>
                            </div>
                          </div>
                          <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                            <div className="flex items-center justify-between gap-3">
                              <div>
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Nova area</p>
                                <p className="mt-1 text-sm text-[var(--text-secondary)]">Adicione uma nova entrada na shell sem deploy.</p>
                              </div>
                              <button
                                type="button"
                                className="pill-button"
                                onClick={() => void handleNavigationItemCreate()}
                                disabled={!canManageSettings || creatingNavigationItem}
                              >
                                {creatingNavigationItem ? 'Criando...' : 'Criar area'}
                              </button>
                            </div>
                            <div className="mt-4 grid gap-3 lg:grid-cols-2">
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">ID</span>
                                <input
                                  value={navigationItemDraft.id}
                                  onChange={(event) => setNavigationItemDraft((current) => ({ ...current, id: event.target.value }))}
                                  disabled={!canManageSettings || creatingNavigationItem}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Nome da nova area</span>
                                <input
                                  aria-label="Nome da nova area"
                                  value={navigationItemDraft.label}
                                  onChange={(event) => setNavigationItemDraft((current) => ({ ...current, label: event.target.value }))}
                                  disabled={!canManageSettings || creatingNavigationItem}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Rota da nova area</span>
                                <input
                                  aria-label="Rota da nova area"
                                  value={navigationItemDraft.path}
                                  onChange={(event) => setNavigationItemDraft((current) => ({ ...current, path: event.target.value }))}
                                  disabled={!canManageSettings || creatingNavigationItem}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Icone da nova area</span>
                                <select
                                  aria-label="Icone da nova area"
                                  value={navigationItemDraft.icon}
                                  onChange={(event) => setNavigationItemDraft((current) => ({ ...current, icon: event.target.value as ShellIconKey }))}
                                  disabled={!canManageSettings || creatingNavigationItem}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                >
                                  {shellIconOptions.map((icon) => (
                                    <option key={icon.value} value={icon.value}>
                                      {icon.label}
                                    </option>
                                  ))}
                                </select>
                              </label>
                              <label className="grid gap-2 lg:col-span-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao da nova area</span>
                                <input
                                  aria-label="Descricao da nova area"
                                  value={navigationItemDraft.description}
                                  onChange={(event) => setNavigationItemDraft((current) => ({ ...current, description: event.target.value }))}
                                  disabled={!canManageSettings || creatingNavigationItem}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                            </div>
                          </div>
                          <div className="mt-4 space-y-3">
                            {shellCatalog.items.map((item) => (
                              <div key={item.id} className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                  <div className="min-w-0">
                                    <div className="flex flex-wrap items-center gap-2">
                                      <p className="text-sm font-semibold text-[var(--text-primary)]">{item.id}</p>
                                      <StatusBadge state={item.enabled ? item.availability : 'restricted'} />
                                    </div>
                                    <p className="mt-1 text-xs text-[var(--text-tertiary)]">{item.path}</p>
                                  </div>
                                  <button
                                    type="button"
                                    className="pill-button"
                                    onClick={() => void handleNavigationItemSave(item)}
                                    disabled={!canManageSettings || savingNavigationItemId === item.id}
                                  >
                                    {savingNavigationItemId === item.id ? 'Salvando...' : 'Salvar area'}
                                  </button>
                                </div>

                                <div className="mt-4 grid gap-3 lg:grid-cols-2">
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Nome</span>
                                    <input
                                      value={item.label}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, label: event.target.value }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Rota</span>
                                    <input
                                      value={item.path}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, path: event.target.value }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Icone</span>
                                    <select
                                      value={item.icon}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, icon: event.target.value as ShellIconKey }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    >
                                      {shellIconOptions.map((icon) => (
                                        <option key={icon.value} value={icon.value}>
                                          {icon.label}
                                        </option>
                                      ))}
                                    </select>
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Grupo</span>
                                    <select
                                      value={item.group}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, group: event.target.value as WorkspaceGroup }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    >
                                      <option value="primary">primary</option>
                                      <option value="task-history">task-history</option>
                                      <option value="secondary">secondary</option>
                                    </select>
                                  </label>
                                  <label className="grid gap-2 lg:col-span-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao</span>
                                    <input
                                      value={item.description}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, description: event.target.value }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Ordem</span>
                                    <input
                                      type="number"
                                      min={0}
                                      value={item.sortOrder}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, sortOrder: Number.parseInt(event.target.value || '0', 10) }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Disponibilidade</span>
                                    <select
                                      value={item.availability}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, availability: event.target.value as WorkspaceAvailabilityState }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    >
                                      <option value="live">live</option>
                                      <option value="attention">attention</option>
                                      <option value="unavailable">unavailable</option>
                                      <option value="restricted">restricted</option>
                                    </select>
                                  </label>
                                  <label className="grid gap-2 lg:col-span-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Palavras-chave</span>
                                    <input
                                      value={item.keywords.join(', ')}
                                      onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({
                                        ...current,
                                        keywords: event.target.value.split(',').map((value) => value.trim()).filter(Boolean),
                                      }))}
                                      disabled={!canManageSettings || savingNavigationItemId === item.id}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                </div>

                                <label className="mt-4 flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                                  <span>
                                    <p className="text-sm font-semibold text-[var(--text-primary)]">Exibir no workspace</p>
                                    <p className="text-sm text-[var(--text-secondary)]">Quando desligado, a area sai da navegacao principal.</p>
                                  </span>
                                  <input
                                    type="checkbox"
                                    checked={item.enabled}
                                    onChange={(event) => updateShellCatalogItemDraft(item.id, (current) => ({ ...current, enabled: event.target.checked }))}
                                    disabled={!canManageSettings || savingNavigationItemId === item.id}
                                  />
                                </label>
                                <div className="mt-3 flex justify-end">
                                  <button
                                    type="button"
                                    className="pill-button"
                                    onClick={() => void handleNavigationItemDelete(item)}
                                    disabled={!canManageSettings || savingNavigationItemId === item.id}
                                  >
                                    <FiTrash2 size={14} />
                                    Remover area
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        </article>

                        <article className="shell-panel p-5">
                          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Tipos de tarefa</p>
                          <p className="mt-2 text-sm text-[var(--text-secondary)]">Defina os nomes e a ordem das tarefas sugeridas no fluxo principal.</p>
                          <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                            <div className="flex items-center justify-between gap-3">
                              <div>
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Novo tipo</p>
                                <p className="mt-1 text-sm text-[var(--text-secondary)]">Adicione um novo tipo de tarefa para o fluxo principal.</p>
                              </div>
                              <button
                                type="button"
                                className="pill-button"
                                onClick={() => void handleTaskTypeCreate()}
                                disabled={!canManageSettings || creatingTaskType}
                              >
                                {creatingTaskType ? 'Criando...' : 'Criar tipo'}
                              </button>
                            </div>
                            <div className="mt-4 grid gap-3">
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">ID do tipo</span>
                                <input
                                  value={taskTypeDraft.taskType}
                                  onChange={(event) => setTaskTypeDraft((current) => ({ ...current, taskType: event.target.value }))}
                                  disabled={!canManageSettings || creatingTaskType}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Nome do novo tipo</span>
                                <input
                                  aria-label="Nome do novo tipo"
                                  value={taskTypeDraft.label}
                                  onChange={(event) => setTaskTypeDraft((current) => ({ ...current, label: event.target.value }))}
                                  disabled={!canManageSettings || creatingTaskType}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                              <label className="grid gap-2">
                                <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao do novo tipo</span>
                                <input
                                  aria-label="Descricao do novo tipo"
                                  value={taskTypeDraft.description}
                                  onChange={(event) => setTaskTypeDraft((current) => ({ ...current, description: event.target.value }))}
                                  disabled={!canManageSettings || creatingTaskType}
                                  className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                  style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                />
                              </label>
                            </div>
                          </div>
                          <div className="mt-4 space-y-3">
                            {shellCatalog.taskTypes.map((item) => (
                              <div key={item.taskType} className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                                <div className="flex flex-wrap items-center justify-between gap-3">
                                  <div>
                                    <p className="text-sm font-semibold text-[var(--text-primary)]">{item.taskType}</p>
                                    <p className="mt-1 text-xs text-[var(--text-tertiary)]">Usado na home e na criacao de tarefa.</p>
                                  </div>
                                  <button
                                    type="button"
                                    className="pill-button"
                                    onClick={() => void handleTaskTypeSave(item)}
                                    disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                  >
                                    {savingTaskTypeId === item.taskType ? 'Salvando...' : 'Salvar tipo'}
                                  </button>
                                </div>

                                <div className="mt-4 grid gap-3">
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Nome</span>
                                    <input
                                      value={item.label}
                                      onChange={(event) => updateShellTaskTypeDraft(item.taskType, (current) => ({ ...current, label: event.target.value }))}
                                      disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Descricao</span>
                                    <input
                                      value={item.description}
                                      onChange={(event) => updateShellTaskTypeDraft(item.taskType, (current) => ({ ...current, description: event.target.value }))}
                                      disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="grid gap-2">
                                    <span className="text-sm font-semibold text-[var(--text-primary)]">Ordem</span>
                                    <input
                                      type="number"
                                      min={0}
                                      value={item.sortOrder}
                                      onChange={(event) => updateShellTaskTypeDraft(item.taskType, (current) => ({ ...current, sortOrder: Number.parseInt(event.target.value || '0', 10) }))}
                                      disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                      className="rounded-[12px] border px-4 py-3 text-sm outline-none"
                                      style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)', color: 'var(--text-primary)' }}
                                    />
                                  </label>
                                  <label className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--surface-bg)' }}>
                                    <span>
                                      <p className="text-sm font-semibold text-[var(--text-primary)]">Exibir nas sugestoes</p>
                                      <p className="text-sm text-[var(--text-secondary)]">Quando desligado, esse tipo deixa de aparecer como opcao principal.</p>
                                    </span>
                                    <input
                                      type="checkbox"
                                      checked={item.enabled}
                                      onChange={(event) => updateShellTaskTypeDraft(item.taskType, (current) => ({ ...current, enabled: event.target.checked }))}
                                      disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                    />
                                  </label>
                                  <div className="flex justify-end">
                                    <button
                                      type="button"
                                      className="pill-button"
                                      onClick={() => void handleTaskTypeDelete(item)}
                                      disabled={!canManageSettings || savingTaskTypeId === item.taskType}
                                    >
                                      <FiTrash2 size={14} />
                                      Remover tipo
                                    </button>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        </article>
                      </div>
                    ) : null}

                    <section className="manus-banner">
                      <div className="flex items-start gap-3">
                        <FiSettings size={18} className="mt-0.5 text-[var(--accent)]" />
                        <div>
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Mudancas aplicadas sem deploy</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
                            Ao salvar, a barra lateral e a tela inicial refletem a nova ordem do workspace imediatamente.
                          </p>
                        </div>
                      </div>
                    </section>
                  </section>
                ) : null}

                {isThreatSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-3">
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Fontes</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{threatIntelCards.length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Confianca</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Auditado</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Status</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Restrito</p></div>
                    </div>

                    {!canReadThreatIntel ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Esta area fica disponivel apenas para perfis autorizados.</div> : null}
                    {canReadThreatIntel ? (
                      <div className="grid gap-4 xl:grid-cols-2">
                        {threatIntelCards.map((card) => (
                          <article key={card.provider.code} className="shell-panel p-5">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="text-lg font-semibold text-[var(--text-primary)]">{card.provider.name}</p>
                              <StatusBadge state="restricted" />
                            </div>
                            <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">{card.provider.notes}</p>
                            <p className="mt-4 text-sm text-[var(--text-primary)]">{credentialSummary(card)}</p>
                            <div className="mt-4 flex flex-wrap gap-3">
                              <ExternalLink href={card.provider.docsUrl} label="Docs" />
                              <ExternalLink href={card.provider.apiKeyPortalUrl} label="Portal" />
                            </div>
                            <p className="mt-4 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
                              {humanizeToken(card.provider.implementationStatus)} | {humanizeToken(card.provider.evidenceLevel)}
                            </p>
                          </article>
                        ))}
                      </div>
                    ) : null}

                    <section className="manus-banner">
                      <div className="flex items-start gap-3">
                        <FiShield size={18} className="mt-0.5 text-[var(--accent)]" />
                        <div>
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Acesso restrito e auditado</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">Esta area so aparece para perfis autorizados e mostra apenas o que ja pode ser acompanhado com seguranca.</p>
                        </div>
                      </div>
                    </section>
                  </section>
                ) : null}

                {!isConfigSection && !isKnowledgeSection && !isFinopsSection && !isProvidersSection && !isWorkspaceCatalogSection && !isThreatSection ? (
                  <section className="grid gap-4 lg:grid-cols-2">
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Estado da secao</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{isLiveSection ? 'Ativo' : 'Indisponivel'}</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{isLiveSection ? 'Esta area ja esta disponivel para uso.' : 'Esta area volta para a navegacao quando estiver pronta.'}</p>
                    </div>
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso operacional</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.usage.remainingCredits}/{overview.usage.dailyCredits}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">A mesma leitura exibida na topbar e na pagina de uso.</p>
                    </div>
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Fontes de conhecimento</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.knowledgeSources}</p>
                    </div>
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Inbox</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.unreadNotifications}</p>
                    </div>
                  </section>
                ) : null}

                <section className="manus-banner">
                  <div className="flex items-start gap-3">
                    <FiCheckCircle size={18} className="mt-0.5 text-[var(--accent)]" />
                    <div>
                      <p className="text-sm font-semibold text-[var(--text-primary)]">Somente o que esta disponivel aparece aqui</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">Modulos incompletos ficam fora da navegacao principal e voltam apenas quando estiverem prontos para uso.</p>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </section>
        ) : null}
      </AsyncState>
    </div>
  );
}
