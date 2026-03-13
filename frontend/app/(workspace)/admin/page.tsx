"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { listProviderHealth, listProviderReadiness, listProviderStatus, testProviderConnectivity } from "@/lib/api/providers";
import {
  createByokConnection,
  getAuditFeed,
  getFinopsScorecard,
  getSettingsUiOptions,
  listByokConnections,
  listFinopsAnomalies,
  validateByokConnection
} from "@/lib/api/workspace";

export default function AdminPage() {
  const [providerCode, setProviderCode] = useState("");
  const [connectionName, setConnectionName] = useState("");
  const [secretRef, setSecretRef] = useState("");
  const [scopeLabel, setScopeLabel] = useState("");

  const statusQuery = useQuery({
    queryKey: ["admin-provider-status"],
    queryFn: listProviderStatus
  });

  const healthQuery = useQuery({
    queryKey: ["admin-provider-health"],
    queryFn: () => listProviderHealth() as Promise<
      Array<{
        providerCode: string;
        providerName: string;
        readinessStatus: string;
        message: string;
      }>
    >
  });

  const readinessQuery = useQuery({
    queryKey: ["admin-provider-readiness"],
    queryFn: listProviderReadiness
  });

  const connectivityMutation = useMutation({
    mutationFn: testProviderConnectivity
  });

  const anomaliesQuery = useQuery({
    queryKey: ["admin-finops-anomalies"],
    queryFn: listFinopsAnomalies
  });
  const scorecardQuery = useQuery({
    queryKey: ["admin-finops-scorecard"],
    queryFn: getFinopsScorecard
  });

  const byokQuery = useQuery({
    queryKey: ["admin-byok-connections"],
    queryFn: listByokConnections
  });

  const auditFeedQuery = useQuery({
    queryKey: ["admin-audit-feed"],
    queryFn: () => getAuditFeed(12)
  });

  const uiOptionsQuery = useQuery({
    queryKey: ["settings-ui-options"],
    queryFn: getSettingsUiOptions
  });

  const byokProviderOptions = useMemo(
    () => uiOptionsQuery.data?.byokProviders ?? [],
    [uiOptionsQuery.data]
  );
  const byokScopeOptions = useMemo(
    () => uiOptionsQuery.data?.byokScopeOptions ?? [],
    [uiOptionsQuery.data]
  );

  const selectedByokProvider = useMemo(
    () => byokProviderOptions.find((provider) => provider.providerCode === providerCode) ?? null,
    [byokProviderOptions, providerCode]
  );

  useEffect(() => {
    if (providerCode || byokProviderOptions.length === 0) {
      return;
    }
    const fallbackProvider = byokProviderOptions[0];
    setProviderCode(fallbackProvider.providerCode);
    setConnectionName(`byok-${fallbackProvider.providerCode}`);
    setSecretRef(fallbackProvider.secretRefSuggestion);
  }, [providerCode, byokProviderOptions]);

  useEffect(() => {
    if (scopeLabel || byokScopeOptions.length === 0) {
      return;
    }
    const defaultScope = byokScopeOptions.find((scope) => scope.defaultOption) ?? byokScopeOptions[0];
    setScopeLabel(defaultScope.code);
  }, [scopeLabel, byokScopeOptions]);

  function handleByokProviderChange(nextProviderCode: string) {
    setProviderCode(nextProviderCode);
    const nextProvider = byokProviderOptions.find((provider) => provider.providerCode === nextProviderCode);
    if (!nextProvider) {
      setConnectionName("");
      setSecretRef("");
      return;
    }
    setConnectionName((current) => current || `byok-${nextProvider.providerCode}`);
    setSecretRef(nextProvider.secretRefSuggestion);
  }

  const createByokMutation = useMutation({
    mutationFn: createByokConnection,
    onSuccess: () => byokQuery.refetch()
  });

  const validateByokMutation = useMutation({
    mutationFn: validateByokConnection,
    onSuccess: () => byokQuery.refetch()
  });

  function submitByok(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!providerCode.trim() || !connectionName.trim() || !secretRef.trim() || !scopeLabel.trim()) {
      return;
    }
    createByokMutation.mutate({
      providerCode: providerCode.trim(),
      connectionName: connectionName.trim(),
      secretRef: secretRef.trim().toUpperCase(),
      scopeLabel: scopeLabel.trim()
    });
  }

  function severityVariant(severity: string): "active" | "attention" | "unavailable" | "restricted" {
    if (severity === "critical") {
      return "unavailable";
    }
    if (severity === "warning") {
      return "attention";
    }
    return "active";
  }

  function healthVariant(healthStatus: string): "active" | "attention" | "unavailable" | "restricted" {
    if (healthStatus === "healthy") {
      return "active";
    }
    if (healthStatus === "unknown") {
      return "attention";
    }
    return "unavailable";
  }

  const tierSummary = (statusQuery.data ?? []).reduce<Record<string, number>>((accumulator, provider) => {
    const tier = provider.providerTier || "catalog_only";
    accumulator[tier] = (accumulator[tier] ?? 0) + 1;
    return accumulator;
  }, {});

  return (
    <>
      <PageHeader
        title="Admin e FinOps"
        description="Readiness dos providers, saude operacional e validacao de conectividade."
      />
      <div className="grid-2">
        <Panel title="Scorecard executivo" subtitle="Sinais operacionais reais para ativacao, billing e governanca.">
          {scorecardQuery.isLoading ? (
            <p className="muted">Carregando scorecard...</p>
          ) : scorecardQuery.data ? (
            <div className="item-list">
              <div className="item-row">
                <strong>Ativacao e execucao</strong>
                <p>
                  TTFV {scorecardQuery.data.activationMinutesToFirstTaskMedian ?? "n/d"} min · conclusao {scorecardQuery.data.taskCompletionRate ?? "n/d"}% ·
                  sucesso de inferencia {scorecardQuery.data.inferenceSuccessRate ?? "n/d"}%
                </p>
                <p className="muted">
                  WAU {scorecardQuery.data.weeklyActiveUsers ?? "n/d"} · MAU {scorecardQuery.data.monthlyActiveUsers ?? "n/d"}
                </p>
              </div>
              <div className="item-row">
                <strong>Billing e reconciliacao</strong>
                <p>
                  Invoices pagas {scorecardQuery.data.paidInvoicesCount} · falhas {scorecardQuery.data.paymentFailureCount} ·
                  orfaos {scorecardQuery.data.orphanPaymentEvents} · pendentes {scorecardQuery.data.pendingPaymentEvents}
                </p>
                <p className="muted">
                  {scorecardQuery.data.reconciliationStatus ?? "Sem reconciliacao"} · drift {scorecardQuery.data.creditDrift ?? "n/d"} ·
                  ultima execucao {scorecardQuery.data.lastReconciledAt ?? "nao registrada"}
                </p>
              </div>
              <div className="item-row">
                <strong>Governanca</strong>
                <p>
                  Tickets abertos {scorecardQuery.data.openSupportTickets} · criticos {scorecardQuery.data.criticalOpenSupportTickets} ·
                  fora do SLA {scorecardQuery.data.overdueSupportTickets} · BYOK saudavel {scorecardQuery.data.healthyByokConnections}/{scorecardQuery.data.byokConnections}
                </p>
                <p className="muted">{scorecardQuery.data.notes}</p>
              </div>
            </div>
          ) : (
            <EmptyState title="Scorecard indisponivel" description="Nao foi possivel carregar o scorecard de FinOps." />
          )}
        </Panel>

        <Panel title="Tiers de providers" subtitle="Distribuicao atual entre core, restritos e bloqueados.">
          {statusQuery.isLoading ? (
            <p className="muted">Carregando tiers...</p>
          ) : (
            <div className="item-list">
              <div className="item-row">
                <strong>Resumo</strong>
                <p>
                  Core live {tierSummary.core_live ?? 0} · Restritos {tierSummary.supported_restricted ?? 0} ·
                  Bloqueados {tierSummary.blocked ?? 0} · Catalog only {tierSummary.catalog_only ?? 0}
                </p>
              </div>
              <div className="item-row">
                <strong>Regra operacional</strong>
                <p>Provider so entra em destaque quando readiness, credencial e smoke estao coerentes.</p>
              </div>
            </div>
          )}
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Status de providers" subtitle="Configuracao e prontidao por provider.">
          {statusQuery.isLoading ? (
            <p className="muted">Carregando status...</p>
          ) : statusQuery.data && statusQuery.data.length > 0 ? (
            <div className="item-list">
              {statusQuery.data.map((provider) => (
                <div className="item-row" key={provider.providerCode}>
                  <strong>{provider.providerName}</strong>
                  <p>
                    {provider.category} · {provider.readinessStatus}
                  </p>
                  <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                    <StatusBadge
                      label={provider.configured ? "Ativo" : "Atencao"}
                      variant={provider.configured ? "active" : "attention"}
                    />
                    <StatusBadge
                      label={provider.providerTier}
                      variant={
                        provider.providerTier === "core_live"
                          ? "active"
                          : provider.providerTier === "supported_restricted"
                            ? "attention"
                            : provider.providerTier === "blocked"
                              ? "restricted"
                              : "attention"
                      }
                    />
                  </div>
                  <div style={{ marginTop: "0.45rem" }}>
                    <button
                      className="button-secondary"
                      type="button"
                      onClick={() => connectivityMutation.mutate(provider.providerCode)}
                      disabled={connectivityMutation.isPending}
                    >
                      Testar conectividade
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem providers" description="Nenhum provider encontrado no catalogo." />
          )}
        </Panel>

        <Panel title="Readiness de providers" subtitle="Tier, smoke status e blockers em um contrato unico.">
          {readinessQuery.isLoading ? (
            <p className="muted">Carregando readiness...</p>
          ) : readinessQuery.data && readinessQuery.data.length > 0 ? (
            <div className="item-list">
              {readinessQuery.data.map((provider) => (
                <div className="item-row" key={provider.providerCode}>
                  <strong>{provider.providerName}</strong>
                  <p>
                    {provider.providerTier} · {provider.readinessStatus} · smoke {provider.smokeStatus}
                  </p>
                  <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                    <StatusBadge
                      label={provider.providerTier}
                      variant={
                        provider.providerTier === "core_live"
                          ? "active"
                          : provider.providerTier === "supported_restricted"
                            ? "attention"
                            : provider.providerTier === "blocked"
                              ? "restricted"
                              : "attention"
                      }
                    />
                    <StatusBadge
                      label={provider.smokeStatus}
                      variant={provider.smokeStatus === "verified_live" ? "active" : provider.smokeStatus === "blocked" ? "restricted" : "attention"}
                    />
                  </div>
                  <p className="muted">{provider.blockerMessage ?? "Sem bloqueios operacionais registrados."}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem readiness" description="Nenhum provider encontrado para o feed operacional." />
          )}
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Health dos providers" subtitle="Snapshots de saude para operacao e suporte.">
          {healthQuery.isLoading ? (
            <p className="muted">Carregando health...</p>
          ) : healthQuery.data && healthQuery.data.length > 0 ? (
            <div className="item-list">
              {healthQuery.data.map((health) => (
                <div className="item-row" key={health.providerCode}>
                  <strong>{health.providerName}</strong>
                  <p>{health.readinessStatus}</p>
                  <p className="muted">{health.message}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem snapshots" description="Nenhum snapshot de health disponivel." />
          )}
          {connectivityMutation.data ? (
            <p className="message-info">
              Teste: {(connectivityMutation.data as { providerName: string; status: string }).providerName} ·{" "}
              {(connectivityMutation.data as { providerName: string; status: string }).status}
            </p>
          ) : null}
        </Panel>

        <Panel title="Feed de auditoria" subtitle="Mudancas administrativas recentes do workspace.">
          {auditFeedQuery.isLoading ? (
            <p className="muted">Carregando auditoria...</p>
          ) : auditFeedQuery.data && auditFeedQuery.data.length > 0 ? (
            <div className="item-list">
              {auditFeedQuery.data.map((entry) => (
                <div className="item-row" key={entry.id}>
                  <strong>{entry.entityType} · {entry.action}</strong>
                  <p>{entry.entityId}</p>
                  <p className="muted">{entry.createdAt ?? "Sem timestamp"}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem eventos recentes" description="A trilha administrativa ainda nao registrou eventos para este workspace." />
          )}
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Anomalias FinOps" subtitle="Sinais de risco de custo, budget e conciliacao.">
          {anomaliesQuery.isLoading ? (
            <p className="muted">Carregando anomalias...</p>
          ) : anomaliesQuery.data && anomaliesQuery.data.length > 0 ? (
            <div className="item-list">
              {anomaliesQuery.data.map((anomaly) => (
                <div className="item-row" key={anomaly.code}>
                  <strong>{anomaly.title}</strong>
                  <p>{anomaly.detail}</p>
                  <StatusBadge label={anomaly.severity} variant={severityVariant(anomaly.severity)} />
                  <p className="muted">{anomaly.recommendedAction}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem anomalias abertas" description="FinOps sem alertas relevantes no momento." />
          )}
        </Panel>

        <Panel title="BYOK (piloto)" subtitle="Conexoes por workspace com validacao de segredo.">
          <form className="form-grid" onSubmit={submitByok}>
            <label>
              Provider code
              <select value={providerCode} onChange={(event) => handleByokProviderChange(event.target.value)}>
                <option value="">Selecione</option>
                {byokProviderOptions.map((provider) => (
                  <option key={provider.providerCode} value={provider.providerCode}>
                    {provider.providerName} ({provider.providerCode})
                  </option>
                ))}
              </select>
            </label>
            <label>
              Connection name
              <input value={connectionName} onChange={(event) => setConnectionName(event.target.value)} />
            </label>
            <label>
              Secret ref (env var)
              <input value={secretRef} onChange={(event) => setSecretRef(event.target.value)} />
            </label>
            <label>
              Scope label
              <select value={scopeLabel} onChange={(event) => setScopeLabel(event.target.value)}>
                <option value="">Selecione</option>
                {byokScopeOptions.map((scope) => (
                  <option key={scope.code} value={scope.code}>
                    {scope.label}
                  </option>
                ))}
              </select>
            </label>
            <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
              <button
                className="button-primary"
                type="submit"
                disabled={
                  createByokMutation.isPending ||
                  !providerCode.trim() ||
                  !connectionName.trim() ||
                  !secretRef.trim() ||
                  !scopeLabel.trim() ||
                  byokProviderOptions.length === 0 ||
                  byokScopeOptions.length === 0
                }
              >
                {createByokMutation.isPending ? "Criando conexao..." : "Criar conexao BYOK"}
              </button>
            </div>
          </form>
          {selectedByokProvider ? (
            <p className="muted" style={{ marginTop: "0.5rem" }}>
              Sugestao de segredo: {selectedByokProvider.secretRefSuggestion}
            </p>
          ) : null}

          {createByokMutation.error ? (
            <p className="message-info">{(createByokMutation.error as Error).message}</p>
          ) : null}

          {byokQuery.isLoading ? (
            <p className="muted">Carregando conexoes...</p>
          ) : byokQuery.data && byokQuery.data.length > 0 ? (
            <div className="item-list">
              {byokQuery.data.map((connection) => (
                <div className="item-row" key={connection.id}>
                  <strong>{connection.connectionName}</strong>
                  <p>
                    {connection.providerName} · {connection.scopeLabel}
                  </p>
                  <StatusBadge label={connection.healthStatus} variant={healthVariant(connection.healthStatus)} />
                  <p className="muted">{connection.lastError ?? "Sem erros de validacao."}</p>
                  <div style={{ marginTop: "0.45rem" }}>
                    <button
                      className="button-secondary"
                      type="button"
                      onClick={() => validateByokMutation.mutate(connection.id)}
                      disabled={validateByokMutation.isPending}
                    >
                      Validar segredo
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem conexoes BYOK" description="Crie a primeira conexao BYOK para este workspace." />
          )}
        </Panel>
      </div>
    </>
  );
}
