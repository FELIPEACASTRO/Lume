import { useEffect, useMemo, useState } from 'react';
import {
  FiCheckCircle,
  FiChevronRight,
  FiExternalLink,
  FiMail,
  FiMoon,
  FiRefreshCcw,
  FiSettings,
  FiShield,
  FiSun,
} from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { useTheme } from '../components/theme/ThemeProvider';
import { toApiClientError } from '../services/api';
import { providerService } from '../services/providerService';
import { settingsService } from '../services/settingsService';
import {
  PreviewState,
  ProviderConnectivityDto,
  ProviderCredentialDto,
  ProviderDto,
  ProviderHealthDto,
  ProviderStatusDto,
  SettingsOverviewDto,
  ThemeMode,
} from '../types';

type ProviderCard = {
  provider: ProviderDto;
  status?: ProviderStatusDto;
  credentials?: ProviderCredentialDto;
  health?: ProviderHealthDto;
  connectivity?: ProviderConnectivityDto;
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

  const isConfigSection = activeSection?.key === 'configuracoes';
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

                {!isConfigSection && !isProvidersSection && !isThreatSection ? (
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
