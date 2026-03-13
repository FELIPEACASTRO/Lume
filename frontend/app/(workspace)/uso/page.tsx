"use client";

import { useQuery } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { getFinopsScorecard, getUsageSummary } from "@/lib/api/workspace";

export default function UsagePage() {
  const usageQuery = useQuery({
    queryKey: ["usage-summary"],
    queryFn: getUsageSummary
  });

  const scorecardQuery = useQuery({
    queryKey: ["finops-scorecard"],
    queryFn: getFinopsScorecard
  });

  return (
    <>
      <PageHeader
        title="Uso e budgets"
        description="Consumo operacional, limites de credito e scorecard de eficiencia."
      />

      <div className="grid-3">
        <article className="metric-card">
          <strong>{usageQuery.data?.dailyCredits ?? 0}</strong>
          <span>Creditos diarios</span>
        </article>
        <article className="metric-card">
          <strong>{usageQuery.data?.consumedCredits ?? 0}</strong>
          <span>Creditos consumidos</span>
        </article>
        <article className="metric-card">
          <strong>{usageQuery.data?.remainingCredits ?? 0}</strong>
          <span>Creditos restantes</span>
        </article>
      </div>

      <div className="grid-2">
        <Panel title="Budget atual" subtitle="Visao do budget aplicado ao workspace.">
          {!usageQuery.data ? (
            <p className="muted">Carregando budget...</p>
          ) : (
            <div className="item-list">
              <div className="item-row">
                <strong>Centro de custo</strong>
                <p>{usageQuery.data.budget.costCenter}</p>
              </div>
              <div className="item-row">
                <strong>Status</strong>
                <p>{usageQuery.data.budget.budgetStatus}</p>
              </div>
              <div className="item-row">
                <strong>Utilizacao soft/hard</strong>
                <p>
                  {usageQuery.data.budget.softLimitUtilizationPercent}% /{" "}
                  {usageQuery.data.budget.hardLimitUtilizationPercent}%
                </p>
              </div>
            </div>
          )}
        </Panel>

        <Panel title="Scorecard FinOps" subtitle="Indicadores operacionais para decisao de custo/qualidade.">
          {!scorecardQuery.data ? (
            <p className="muted">Carregando scorecard...</p>
          ) : (
            <div className="item-list">
              <div className="item-row">
                <strong>Retencao D30</strong>
                <p>{scorecardQuery.data.d30RetentionRate ?? 0}%</p>
              </div>
              <div className="item-row">
                <strong>Margem de contribuicao</strong>
                <p>{scorecardQuery.data.marginContributionRate ?? 0}%</p>
              </div>
              <div className="item-row">
                <strong>Custo medio estimado por run (USD)</strong>
                <p>{scorecardQuery.data.averageEstimatedCostUsdPerRun ?? 0}</p>
              </div>
            </div>
          )}
        </Panel>
      </div>
    </>
  );
}
