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
import { knowledgeSourceService } from '../services/knowledgeSourceService';
import { providerService } from '../services/providerService';
import { projectService } from '../services/projectService';
import { settingsService } from '../services/settingsService';
import {
  BudgetSummaryDto,
  ChargebackMode,
  CreateKnowledgeSourceRequest,
  KnowledgeSourceDto,
  PreviewState,
  ProjectDto,
  ProviderConnectivityDto,
  ProviderCredentialDto,
  ProviderDto,
  ProviderHealthDto,
  ProviderStatusDto,
  SettingsOverviewDto,
  ThemeMode,
  UpdateKnowledgeSourceRequest,
} from '../types';

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

function providerState(card: ProviderCard): PreviewState {
  if (!(card.health?.executionSupported ?? card.status?.executionSupported ?? card.provider.executionSupported)) {
    return 'preview';
  }
  return (card.health?.configured ?? card.status?.configured ?? card.provider.configured) ? 'live' : 'disabled-preview';
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
    return 'Readiness indisponivel.';
  }
  return readiness.replace(/_/g, ' ');
}

function providerStreamingMode(card: ProviderCard) {
  return (card.health?.streamingMode ?? card.status?.streamingMode ?? card.provider.streamingMode).replace(/_/g, ' ');
}

function providerRuntimeMaturity(card: ProviderCard) {
  return (card.health?.runtimeMaturity ?? card.status?.runtimeMaturity ?? card.credentials?.runtimeMaturity ?? card.provider.runtimeMaturity).replace(/_/g, ' ');
}

function providerImplementationStatus(card: ProviderCard) {
  return (card.health?.implementationStatus ?? card.status?.implementationStatus ?? card.credentials?.implementationStatus ?? card.provider.implementationStatus).replace(/_/g, ' ');
}

function providerEvidenceLevel(card: ProviderCard) {
  return (card.health?.evidenceLevel ?? card.status?.evidenceLevel ?? card.credentials?.evidenceLevel ?? card.provider.evidenceLevel).replace(/_/g, ' ');
}

function ExternalLink({ href, label }: { href: string; label: string }) {
  return (
    <a className="pill-button" href={href} target="_blank" rel="noreferrer">
      {label}
      <FiExternalLink size={14} />
    </a>
  );
}

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
          <span>{card.provider.category}</span>
          <span>{card.provider.apiStyle}</span>
          <span>{providerImplementationStatus(card)}</span>
          <span>{providerEvidenceLevel(card)}</span>
          <span>{card.provider.catalogState}</span>
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
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Readiness</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{providerReadiness(card)}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">
            {card.health?.message ?? 'Health agregado sem chamadas externas pesadas.'}
          </p>
          {card.health ? (
            <p className="mt-2 text-xs text-[var(--text-tertiary)]">
              Fonte: {card.health.healthSource.replace(/_/g, ' ')} | Snapshot: {card.health.snapshotPersistence}
            </p>
          ) : null}
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Headers</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.requiredHeaders.join(', ') || 'Nenhum header especial.'}</p>
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Governanca</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">
            {providerImplementationStatus(card)} | {providerEvidenceLevel(card)}
          </p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">
            {card.provider.businessPriority.replace(/_/g, ' ')} | {card.provider.syncMode.replace(/_/g, ' ')}
          </p>
        </div>
      </div>

      <div className="mt-3 grid gap-3 xl:grid-cols-2">
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">FinOps</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.pricingSummary}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">{card.provider.rateLimitSummary}</p>
        </div>
        <div className="rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Roteamento</p>
          <p className="mt-2 text-sm text-[var(--text-primary)]">{card.provider.routingModes.join(', ')}</p>
          <p className="mt-2 text-xs text-[var(--text-secondary)]">Fonte documental: {card.provider.documentationSource.replace(/_/g, ' ')}</p>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <ExternalLink href={card.provider.apiKeyPortalUrl} label="Portal" />
        <ExternalLink href={card.provider.docsUrl} label="Docs" />
        {canTest ? (
          <button type="button" className="pill-button" onClick={() => onTest(card.provider.code)} disabled={testing}>
            <FiRefreshCcw size={14} />
            {testing ? 'Testando...' : 'Connectivity test'}
          </button>
        ) : null}
      </div>

      {card.connectivity ? (
        <div className="mt-4 rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
          <p className="text-sm font-semibold text-[var(--text-primary)]">Connectivity test</p>
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
  const { session } = useShell();
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
  const isThreatSection = activeSection?.key === 'threat-intelligence';
  const isLiveSection = activeSection?.previewState === 'live';

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

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Settings em formato modal-page."
        description="A area de configuracoes agora mistura preferencias reais, catalogo de providers e controles administrativos sem fingir que todo provider ja esta live."
        state="preview"
        detail={preferencesError ?? 'Aparencia, idioma, credenciais exigidas e runtime sao resolvidos por APIs reais.'}
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando settings do workspace..."
        errorTitle="As configuracoes nao responderam."
        errorDescription="O overview do workspace nao foi carregado."
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
                          <StatusBadge state={section.previewState} />
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
                    <StatusBadge state={activeSection.previewState} />
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
                            <p className="text-sm text-[var(--text-secondary)]">Campo real vindo de user_preferences.</p>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Workspace</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.workspaceName}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">Role atual: {overview.roleLabel}</p>
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
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Prontas para agents</p>
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
                                Nenhuma fonte de conhecimento foi registrada neste workspace ainda.
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
                                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Agents</p>
                                      <p className="mt-2 text-sm text-[var(--text-primary)]">{source.enabledForAgents ? 'Habilitado' : 'Desligado'}</p>
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
                                <p className="text-sm font-semibold text-[var(--text-primary)]">Disponivel para agents</p>
                                <p className="text-sm text-[var(--text-secondary)]">Permite que o runtime trate a fonte como contexto operacional.</p>
                              </span>
                              <input
                                aria-label="Disponivel para agents"
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
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Foundation real para knowledge plane</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
                            O workspace agora gerencia fontes persistidas com projeto, readiness para agents e trilha de atualizacao, sem prometer retrieval completo antes da camada permissionada de RAG.
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
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Cost center</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget?.costCenter ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Modo</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget?.chargebackMode ?? '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Soft limit</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget ? `${budget.consumedCredits}/${budget.softLimitCredits}` : '--'}</p>
                      </div>
                      <div className="shell-panel p-5">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Hard limit</p>
                        <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{budget ? `${budget.consumedCredits}/${budget.hardLimitCredits}` : '--'}</p>
                      </div>
                    </div>

                    {budgetLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando budget do workspace...</div> : null}
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
                              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Showback/chargeback</p>
                              <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">{budget.chargebackMode}</p>
                              <p className="mt-2 text-xs text-[var(--text-secondary)]">A trilha atual governa o workspace antes do billing comercial final.</p>
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
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Cost center</span>
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
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Soft limit</span>
                              <input
                                aria-label="Soft limit do workspace"
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
                              <span className="text-sm font-semibold text-[var(--text-primary)]">Hard limit</span>
                              <input
                                aria-label="Hard limit do workspace"
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
                              {budgetSaving ? 'Salvando...' : 'Salvar budget'}
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
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Text runtime</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{textRuntimeCards.length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Configurados</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{statuses.filter((item) => item.configured).length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Categorias</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">4</p></div>
                    </div>

                    {providerLoading ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">Carregando catalogo de providers...</div> : null}
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

                {isThreatSection ? (
                  <section className="space-y-4">
                    <div className="grid gap-4 lg:grid-cols-3">
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Escopo</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{threatIntelCards.length}</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Governanca</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Auditado</p></div>
                      <div className="shell-panel p-5"><p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Status</p><p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">Manual / Catalog-only</p></div>
                    </div>

                    {!canReadThreatIntel ? <div className="shell-panel p-5 text-sm text-[var(--text-secondary)]">A visibilidade desta secao depende da permissao `threat_intel.read`.</div> : null}
                    {canReadThreatIntel ? (
                      <div className="grid gap-4 xl:grid-cols-2">
                        {threatIntelCards.map((card) => (
                          <article key={card.provider.code} className="shell-panel p-5">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="text-lg font-semibold text-[var(--text-primary)]">{card.provider.name}</p>
                              <StatusBadge state="disabled-preview" />
                            </div>
                            <p className="mt-3 text-sm leading-6 text-[var(--text-secondary)]">{card.provider.notes}</p>
                            <p className="mt-4 text-sm text-[var(--text-primary)]">{credentialSummary(card)}</p>
                            <div className="mt-4 flex flex-wrap gap-3">
                              <ExternalLink href={card.provider.docsUrl} label="Docs" />
                              <ExternalLink href={card.provider.apiKeyPortalUrl} label="Portal" />
                            </div>
                            <p className="mt-4 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--text-tertiary)]">
                              {card.provider.implementationStatus.replace(/_/g, ' ')} | {card.provider.evidenceLevel.replace(/_/g, ' ')}
                            </p>
                          </article>
                        ))}
                      </div>
                    ) : null}

                    <section className="manus-banner">
                      <div className="flex items-start gap-3">
                        <FiShield size={18} className="mt-0.5 text-[var(--accent)]" />
                        <div>
                          <p className="text-sm font-semibold text-[var(--text-primary)]">Preview honesto, admin-only e sem execucao silenciosa</p>
                          <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">A shell so expõe metadados e links oficiais ate que RBAC, adapters e auditoria estejam completos.</p>
                        </div>
                      </div>
                    </section>
                  </section>
                ) : null}

                {!isConfigSection && !isKnowledgeSection && !isFinopsSection && !isProvidersSection && !isThreatSection ? (
                  <section className="grid gap-4 lg:grid-cols-2">
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Estado da secao</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{isLiveSection ? 'Operacional' : 'Preview visivel'}</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{isLiveSection ? 'Esta area ja tem base real no backend.' : 'A superficie aparece para orientar a experiencia, mas seus controles continuam informativos.'}</p>
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
                      <p className="text-sm font-semibold text-[var(--text-primary)]">Preview honesto e sem side effects</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">As secoes `mail`, `browser`, `skills`, `connectors` e `integrations` seguem visiveis, mas continuam marcadas como preview ate ganharem API, persistencia e testes proprios.</p>
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
