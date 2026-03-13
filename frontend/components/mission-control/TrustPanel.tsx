"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listProviderStatus } from "@/lib/api/providers";
import { getFinopsScorecard } from "@/lib/api/workspace";
import { ProviderHealthBadge } from "./ProviderHealthBadge";
import { PolicyPill } from "./PolicyPill";

export function TrustPanel() {
  const providersQuery = useQuery({
    queryKey: ["mission-control-provider-status"],
    queryFn: listProviderStatus
  });

  const scorecardQuery = useQuery({
    queryKey: ["mission-control-finops-scorecard"],
    queryFn: getFinopsScorecard
  });

  const providerSummary = useMemo(() => {
    const providers = providersQuery.data ?? [];
    return {
      total: providers.length,
      ready: providers.filter((provider) => provider.readinessStatus === "ready").length,
      blocked: providers.filter((provider) => provider.readinessStatus === "blocked").length
    };
  }, [providersQuery.data]);

  return (
    <aside className="trust-panel">
      <section className="trust-panel-section">
        <h3>Trust Panel</h3>
        <p>Sinais operacionais, custo e governança em tempo real.</p>
      </section>

      <section className="trust-panel-section">
        <div className="trust-metric-grid">
          <article className="trust-metric-card">
            <strong>{providerSummary.total}</strong>
            <span>providers monitorados</span>
          </article>
          <article className="trust-metric-card">
            <strong>{providerSummary.ready}</strong>
            <span>readiness ready</span>
          </article>
          <article className="trust-metric-card">
            <strong>{providerSummary.blocked}</strong>
            <span>readiness blocked</span>
          </article>
        </div>
      </section>

      <section className="trust-panel-section">
        <PolicyPill
          label="Reconciliation"
          value={scorecardQuery.data?.reconciliationStatus || "pending"}
        />
        <PolicyPill
          label="Inferência"
          value={scorecardQuery.data?.inferenceSuccessRate == null ? "n/d" : `${scorecardQuery.data.inferenceSuccessRate}%`}
        />
        <PolicyPill
          label="Falhas cobrança"
          value={`${scorecardQuery.data?.paymentFailureCount ?? 0}`}
        />
      </section>

      <section className="trust-panel-section">
        <h4>Top providers</h4>
        <div className="trust-provider-list">
          {(providersQuery.data ?? []).slice(0, 5).map((provider) => (
            <div key={provider.providerCode} className="trust-provider-item">
              <div>
                <strong>{provider.providerName}</strong>
                <p>{provider.providerTier}</p>
              </div>
              <ProviderHealthBadge readinessStatus={provider.readinessStatus} smokeStatus={provider.smokeStatus} />
            </div>
          ))}
        </div>
      </section>
    </aside>
  );
}
