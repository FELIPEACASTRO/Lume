import { useEffect, useMemo, useState } from 'react';
import { FiCheckCircle, FiChevronRight, FiMail, FiMoon, FiSettings, FiSun } from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useTheme } from '../components/theme/ThemeProvider';
import { settingsService } from '../services/settingsService';
import { toApiClientError } from '../services/api';
import { SettingsOverviewDto, ThemeMode } from '../types';

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [overview, setOverview] = useState<SettingsOverviewDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const selectedSection = searchParams.get('section') ?? 'configuracoes';
  const { preferences, preferencesError, updatePreferences } = useTheme();

  const loadOverview = async () => {
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
  };

  useEffect(() => {
    void loadOverview();
  }, []);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (!overview) {
      return 'empty';
    }
    return 'live';
  }, [error, loading, overview]);

  const activeSection = overview?.sections.find((section) => section.key === selectedSection) ?? overview?.sections[0] ?? null;
  const isConfigSection = activeSection?.key === 'configuracoes';
  const isLiveSection = activeSection?.previewState === 'live';

  const handleThemeChange = async (nextTheme: ThemeMode) => {
    try {
      setSaving(true);
      await updatePreferences({ appearance: nextTheme });
      await loadOverview();
    } finally {
      setSaving(false);
    }
  };

  const handleCommunicationToggle = async (key: 'emailUpdates' | 'productUpdates', checked: boolean) => {
    try {
      setSaving(true);
      await updatePreferences({ [key]: checked });
      await loadOverview();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Settings em formato modal-page."
        description="A area de configuracoes agora espelha a linguagem do Manus, mas continua honesta sobre o que ja e real no Lume e o que segue como preview."
        state="preview"
        detail={preferencesError ?? 'Aparencia, idioma e comunicacao agora persistem em user_preferences.'}
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando settings do workspace..."
        errorTitle="As configuracoes nao responderam."
        errorDescription="O overview do workspace nao foi carregado."
        errorDetail={error ?? undefined}
        onRetry={() => void loadOverview()}
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
                        <label className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                          <span>
                            <p className="text-sm font-semibold text-[var(--text-primary)]">Atualizacoes por e-mail</p>
                            <p className="text-sm text-[var(--text-secondary)]">Mudancas relevantes do produto.</p>
                          </span>
                          <input
                            type="checkbox"
                            checked={preferences.emailUpdates}
                            onChange={(event) => void handleCommunicationToggle('emailUpdates', event.target.checked)}
                            disabled={saving}
                          />
                        </label>
                        <label className="flex items-center justify-between rounded-[12px] border px-4 py-4" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
                          <span>
                            <p className="text-sm font-semibold text-[var(--text-primary)]">Atualizacoes de produto</p>
                            <p className="text-sm text-[var(--text-secondary)]">Novos recursos e disponibilidade.</p>
                          </span>
                          <input
                            type="checkbox"
                            checked={preferences.productUpdates}
                            onChange={(event) => void handleCommunicationToggle('productUpdates', event.target.checked)}
                            disabled={saving}
                          />
                        </label>
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

                {!isConfigSection ? (
                  <section className="grid gap-4 lg:grid-cols-2">
                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Estado da secao</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{isLiveSection ? 'Operacional' : 'Preview visivel'}</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
                        {isLiveSection
                          ? 'Esta area ja tem base real no backend e pode seguir evoluindo visualmente sem inventar comportamento.'
                          : 'A superficie aparece para orientar a experiencia, mas seus controles continuam desabilitados ou informativos.'}
                      </p>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Uso operacional</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.usage.remainingCredits}/{overview.usage.dailyCredits}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">A mesma leitura exibida na topbar e na pagina de uso.</p>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Fontes de conhecimento</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.knowledgeSources}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">Memoria operacional pronta para crescer sem prometer integracoes inexistentes.</p>
                    </div>

                    <div className="shell-panel p-5">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--text-tertiary)]">Inbox</p>
                      <p className="mt-3 text-lg font-semibold text-[var(--text-primary)]">{overview.unreadNotifications}</p>
                      <p className="mt-2 text-sm text-[var(--text-secondary)]">Eventos operacionais aguardando tratamento no workspace.</p>
                    </div>
                  </section>
                ) : null}

                <section className="manus-banner">
                  <div className="flex items-start gap-3">
                    <FiCheckCircle size={18} className="mt-0.5 text-[var(--accent)]" />
                    <div>
                      <p className="text-sm font-semibold text-[var(--text-primary)]">Preview honesto e sem side effects</p>
                      <p className="mt-2 text-sm leading-6 text-[var(--text-secondary)]">
                        As secoes `mail`, `browser`, `skills`, `connectors` e `integrations` aparecem como parte da IA do benchmark, mas permanecem claramente identificadas como preview ate ganharem API, persistencia e testes.
                      </p>
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
