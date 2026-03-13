"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { SettingsPreferencesPanel } from "@/components/settings/SettingsPreferencesPanel";
import { SettingsCompliancePanel } from "@/components/settings/SettingsCompliancePanel";
import { listProviderStatus } from "@/lib/api/providers";
import {
  getSettingsOverview,
  getSettingsUiOptions,
  updateSettingsCompliance,
  updateSettingsPreferences
} from "@/lib/api/workspace";
import { toStatusVariant } from "@/lib/utils/statusVariant";

export default function SettingsPage() {
  const searchParams = useSearchParams();
  const focusedSection = searchParams.get("section");
  const queryClient = useQueryClient();
  const [appearance, setAppearance] = useState("light");
  const [languageCode, setLanguageCode] = useState("pt-BR");
  const [emailUpdates, setEmailUpdates] = useState(true);
  const [productUpdates, setProductUpdates] = useState(true);
  const [retentionPolicyStatus, setRetentionPolicyStatus] = useState("");
  const [retentionDays, setRetentionDays] = useState("");
  const [accessReviewStatus, setAccessReviewStatus] = useState("");
  const [accessReviewFrequencyDays, setAccessReviewFrequencyDays] = useState("");
  const [consentTrackingEnabled, setConsentTrackingEnabled] = useState(false);
  const [termsVersion, setTermsVersion] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [complianceFeedback, setComplianceFeedback] = useState<string | null>(null);

  const settingsQuery = useQuery({ queryKey: ["settings-overview"], queryFn: getSettingsOverview });
  const providerStatusQuery = useQuery({ queryKey: ["provider-status"], queryFn: listProviderStatus });
  const uiOptionsQuery = useQuery({ queryKey: ["settings-ui-options"], queryFn: getSettingsUiOptions });

  const retentionStatusOptions = useMemo(() => uiOptionsQuery.data?.complianceRetentionPolicyStatuses ?? [], [uiOptionsQuery.data]);
  const accessReviewStatusOptions = useMemo(() => uiOptionsQuery.data?.complianceAccessReviewStatuses ?? [], [uiOptionsQuery.data]);

  const preferencesMutation = useMutation({
    mutationFn: updateSettingsPreferences,
    onSuccess: () => { setFeedback("Preferencias atualizadas com sucesso."); queryClient.invalidateQueries({ queryKey: ["settings-overview"] }); },
    onError: (error) => { setFeedback(error instanceof Error ? error.message : "Falha ao atualizar preferencias."); }
  });

  const complianceMutation = useMutation({
    mutationFn: updateSettingsCompliance,
    onSuccess: () => { setComplianceFeedback("Controles de compliance atualizados com sucesso."); queryClient.invalidateQueries({ queryKey: ["settings-overview"] }); },
    onError: (error) => { setComplianceFeedback(error instanceof Error ? error.message : "Falha ao atualizar compliance."); }
  });

  useEffect(() => {
    if (!settingsQuery.data) return;
    setAppearance(settingsQuery.data.preferences.appearance);
    setLanguageCode(settingsQuery.data.preferences.languageCode);
    setEmailUpdates(settingsQuery.data.preferences.emailUpdates);
    setProductUpdates(settingsQuery.data.preferences.productUpdates);
    setRetentionPolicyStatus(settingsQuery.data.compliance.retentionPolicyStatus);
    setRetentionDays(settingsQuery.data.compliance.retentionDays?.toString() ?? "");
    setAccessReviewStatus(settingsQuery.data.compliance.accessReviewStatus);
    setAccessReviewFrequencyDays(settingsQuery.data.compliance.accessReviewFrequencyDays?.toString() ?? "");
    setConsentTrackingEnabled(settingsQuery.data.compliance.consentTrackingEnabled);
    setTermsVersion(settingsQuery.data.compliance.termsVersion ?? "");
  }, [settingsQuery.data]);

  useEffect(() => {
    if (retentionPolicyStatus || retentionStatusOptions.length === 0) return;
    const defaultOption = retentionStatusOptions.find((item) => item.defaultOption) ?? retentionStatusOptions[0];
    setRetentionPolicyStatus(defaultOption.code);
  }, [retentionPolicyStatus, retentionStatusOptions]);

  useEffect(() => {
    if (accessReviewStatus || accessReviewStatusOptions.length === 0) return;
    const defaultOption = accessReviewStatusOptions.find((item) => item.defaultOption) ?? accessReviewStatusOptions[0];
    setAccessReviewStatus(defaultOption.code);
  }, [accessReviewStatus, accessReviewStatusOptions]);

  const readyProviders = useMemo(() => (providerStatusQuery.data ?? []).filter((provider) => provider.executionSupported), [providerStatusQuery.data]);
  const providerTierSummary = useMemo(() => {
    return readyProviders.reduce<Record<string, number>>((accumulator, provider) => {
      const tier = provider.providerTier || "catalog_only";
      accumulator[tier] = (accumulator[tier] ?? 0) + 1;
      return accumulator;
    }, {});
  }, [readyProviders]);

  function submitPreferences(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    preferencesMutation.mutate({ appearance, languageCode, emailUpdates, productUpdates });
  }

  function submitCompliance(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    complianceMutation.mutate({
      retentionPolicyStatus,
      retentionDays: retentionPolicyStatus === "configured" && retentionDays.trim() ? Number(retentionDays) : null,
      accessReviewStatus,
      accessReviewFrequencyDays: accessReviewStatus === "configured" && accessReviewFrequencyDays.trim() ? Number(accessReviewFrequencyDays) : null,
      consentTrackingEnabled,
      termsVersion: termsVersion.trim() || null
    });
  }

  if (settingsQuery.isLoading) {
    return (
      <div className="screen-center">
        <div style={{ display: "grid", gap: "0.75rem", width: "280px" }}>
          <div className="skeleton-line" style={{ width: "45%", height: "18px" }} />
          <div className="skeleton-line" style={{ width: "100%" }} />
          <div className="skeleton-line" style={{ width: "70%" }} />
        </div>
      </div>
    );
  }

  if (!settingsQuery.data) {
    return <EmptyState title="Configuracoes indisponiveis" description="Nao foi possivel carregar o resumo do workspace." />;
  }

  const overview = settingsQuery.data;

  return (
    <>
      <PageHeader title="Configuracoes" description="Preferencias, uso, providers e governanca do workspace em uma area unica." />

      <div className="grid-2">
        <Panel title="Resumo do workspace" subtitle="Contexto operacional atual.">
          <div className="item-list">
            <div className="item-row"><strong>Organizacao</strong><p>{overview.organizationName}</p></div>
            <div className="item-row"><strong>Workspace</strong><p>{overview.workspaceName}</p></div>
            <div className="item-row"><strong>Papel atual</strong><p>{overview.roleLabel}</p></div>
            <div className="item-row">
              <strong>Alertas e conhecimento</strong>
              <p>{overview.unreadNotifications} notificacoes abertas · {overview.knowledgeSources} fontes de conhecimento</p>
            </div>
          </div>
        </Panel>

        <SettingsPreferencesPanel
          appearance={appearance}
          languageCode={languageCode}
          emailUpdates={emailUpdates}
          productUpdates={productUpdates}
          isSaving={preferencesMutation.isPending}
          feedback={feedback}
          onAppearanceChange={setAppearance}
          onLanguageCodeChange={setLanguageCode}
          onEmailUpdatesChange={setEmailUpdates}
          onProductUpdatesChange={setProductUpdates}
          onSubmit={submitPreferences}
        />
      </div>

      <div className="grid-2">
        <Panel title="Uso, budget e ativacao" subtitle="Leitura comercial e operacional do workspace.">
          <div className="item-list">
            <div className="item-row">
              <strong>Plano e assinatura</strong>
              <p>{overview.commercial.planLabel} · {overview.commercial.subscriptionStatus}</p>
              <p className="muted">
                {overview.commercial.totalCredits} creditos totais · renovacao {overview.commercial.renewsAt || "nao informada"}
              </p>
            </div>
            <div className="item-row">
              <strong>Ativacao</strong>
              <p>{overview.commercial.activationStatus}</p>
              <p className="muted">{overview.commercial.activationNote}</p>
            </div>
            <div className="item-row">
              <strong>Budget atual</strong>
              <p>{overview.usage.budget.budgetStatus}</p>
              <p className="muted">
                Soft {overview.usage.budget.softLimitCredits} · Hard {overview.usage.budget.hardLimitCredits} · consumido {overview.usage.budget.consumedCredits}
              </p>
            </div>
            <div className="item-row">
              <strong>Consumo diario</strong>
              <p>{overview.usage.consumedCredits} de {overview.usage.dailyCredits} creditos</p>
              <p className="muted">{overview.usage.note}</p>
            </div>
          </div>
        </Panel>

        <Panel title="Providers e readiness" subtitle="Somente status reais do runtime atual.">
          {providerStatusQuery.isLoading ? (
            <p className="muted">Carregando providers...</p>
          ) : readyProviders.length > 0 ? (
            <div className="item-list">
              <div className="item-row">
                <strong>Tiers operacionais</strong>
                <p>
                  Core live {providerTierSummary.core_live ?? 0} · Restritos {providerTierSummary.supported_restricted ?? 0} ·
                  Bloqueados {providerTierSummary.blocked ?? 0}
                </p>
              </div>
              {readyProviders.map((provider) => (
                <div className="item-row" key={provider.providerCode}>
                  <strong>{provider.providerName}</strong>
                  <p>{provider.category} · {provider.providerTier}</p>
                  <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                    <StatusBadge label={provider.readinessStatus} variant={provider.configured ? "active" : "attention"} />
                    <StatusBadge label={provider.providerTier} variant={toStatusVariant(provider.providerTier)} />
                    <StatusBadge label={provider.implementationStatus} variant={provider.executionSupported ? "attention" : "restricted"} />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem providers visiveis" description="Nenhum provider executavel foi retornado para este workspace." />
          )}
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Governanca operacional" subtitle="Rotinas, reconciliacao e controle do workspace.">
          <div className="item-list">
            <div className="item-row">
              <strong>Tickets e suporte</strong>
              <p>
                {overview.governance.openSupportTickets} abertos · {overview.governance.criticalOpenSupportTickets} criticos ·{" "}
                {overview.governance.overdueSupportTickets} fora do SLA
              </p>
            </div>
            <div className="item-row">
              <strong>BYOK</strong>
              <p>{overview.governance.byokConnections} conexoes · {overview.governance.healthyByokConnections} saudaveis</p>
            </div>
            <div className="item-row">
              <strong>Providers por tier</strong>
              <p>
                Core live {overview.governance.coreLiveProviders} · Restritos {overview.governance.supportedRestrictedProviders} ·
                Bloqueados {overview.governance.blockedProviders}
              </p>
            </div>
            <div className="item-row">
              <strong>Ultima reconciliacao</strong>
              <p>{overview.governance.lastReconciliationStatus ?? "Sem execucao registrada"}</p>
              <p className="muted">{overview.governance.lastReconciliationExecutedAt ?? overview.governance.note}</p>
            </div>
          </div>
        </Panel>

        <SettingsCompliancePanel
          compliance={overview.compliance}
          retentionPolicyStatus={retentionPolicyStatus}
          retentionDays={retentionDays}
          accessReviewStatus={accessReviewStatus}
          accessReviewFrequencyDays={accessReviewFrequencyDays}
          consentTrackingEnabled={consentTrackingEnabled}
          termsVersion={termsVersion}
          retentionStatusOptions={retentionStatusOptions}
          accessReviewStatusOptions={accessReviewStatusOptions}
          isSaving={complianceMutation.isPending}
          feedback={complianceFeedback}
          onRetentionPolicyStatusChange={setRetentionPolicyStatus}
          onRetentionDaysChange={setRetentionDays}
          onAccessReviewStatusChange={setAccessReviewStatus}
          onAccessReviewFrequencyDaysChange={setAccessReviewFrequencyDays}
          onConsentTrackingEnabledChange={setConsentTrackingEnabled}
          onTermsVersionChange={setTermsVersion}
          onSubmit={submitCompliance}
        />
      </div>

      <div className="grid-2">
        <Panel title="Secoes disponiveis" subtitle="Mapa do produto vindo do backend.">
          <div className="item-list">
            {overview.sections.map((section) => (
              <div className="item-row" key={section.key}>
                <strong>{section.title}</strong>
                <p>{section.description}</p>
                <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                  <StatusBadge label={section.availability} variant={toStatusVariant(section.availability)} />
                  {focusedSection === section.key ? <StatusBadge label="Foco da busca" variant="attention" /> : null}
                </div>
              </div>
            ))}
          </div>
        </Panel>

        <Panel title="Acessos rapidos" subtitle="Entradas complementares para operacao e suporte.">
          <div className="grid-2">
            <Link className="item-row item-row-link" href="/assinatura">
              <strong>Assinatura</strong><p>Cobranca, invoices e eventos de pagamento.</p>
            </Link>
            <Link className="item-row item-row-link" href="/uso">
              <strong>Uso</strong><p>Leitura de consumo, budgets e creditos.</p>
            </Link>
            <Link className="item-row item-row-link" href="/admin">
              <strong>Admin / FinOps</strong><p>Anomalias, BYOK e operacao administrativa.</p>
            </Link>
            <Link className="item-row item-row-link" href="/ajuda">
              <strong>Ajuda</strong><p>Tickets e rotinas de suporte operacional.</p>
            </Link>
          </div>
        </Panel>
      </div>
    </>
  );
}
