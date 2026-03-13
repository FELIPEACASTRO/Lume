"use client";

type Props = {
  totalEstimatedCostUsd: number | null;
  completedRuns: number;
  failedRuns: number;
};

export function CostMeter({ totalEstimatedCostUsd, completedRuns, failedRuns }: Props) {
  const safeCost = totalEstimatedCostUsd ?? 0;
  const usagePercent = Math.min(100, Math.max(0, safeCost * 1000));

  return (
    <div className="cost-meter">
      <div className="cost-meter-header">
        <span>Custo estimado da rodada</span>
        <strong>
          {totalEstimatedCostUsd == null ? "n/d" : `US$ ${safeCost.toFixed(4)}`}
        </strong>
      </div>
      <div className="cost-meter-bar">
        <span style={{ width: `${usagePercent}%` }} />
      </div>
      <div className="cost-meter-footer">
        <span>{completedRuns} concluídos</span>
        <span>{failedRuns} falhas</span>
      </div>
    </div>
  );
}
