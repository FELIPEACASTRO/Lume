"use client";

import clsx from "clsx";
import { MultiInferenceRun } from "@/lib/api/types";
import { MarkdownRenderer } from "@/components/chat/MarkdownRenderer";
import { ProviderHealthBadge } from "./ProviderHealthBadge";

type Props = {
  run: MultiInferenceRun;
  rank: number;
  winner?: boolean;
  collapsed?: boolean;
  onToggleCollapsed?: () => void;
};

export function RankedResultCard({ run, rank, winner, collapsed, onToggleCollapsed }: Props) {
  const hasContent = !!run.content && run.content.trim().length > 0;

  return (
    <article className={clsx("ranked-result-card", winner && "winner")}>
      <header className="ranked-result-header">
        <div className="ranked-result-title">
          <span className="rank-badge">#{rank}</span>
          <div>
            <strong>{run.providerName || run.providerCode}</strong>
            <p>{run.modelCode} · {run.versionLabel}</p>
          </div>
        </div>
        <div className="ranked-result-meta">
          <ProviderHealthBadge readinessStatus={run.status} />
          <span className="result-score">score {run.score?.toFixed(2) ?? "n/d"}</span>
        </div>
      </header>

      <div className="ranked-result-kpis">
        <span>latência: {run.latencyMs == null ? "n/d" : `${run.latencyMs} ms`}</span>
        <span>custo: {run.estimatedCostUsd == null ? "n/d" : `US$ ${run.estimatedCostUsd.toFixed(6)}`}</span>
        <span>fallback: {run.fallbackUsed ? "sim" : "não"}</span>
      </div>

      {onToggleCollapsed ? (
        <button type="button" className="button-secondary ranked-result-toggle" onClick={onToggleCollapsed}>
          {collapsed ? "Expandir resultado" : "Recolher resultado"}
        </button>
      ) : null}

      {!collapsed ? (
        <div className="ranked-result-body">
          {hasContent ? (
            <MarkdownRenderer content={run.content as string} />
          ) : (
            <p className="message-error">{run.error || "Execução sem conteúdo retornado."}</p>
          )}
        </div>
      ) : null}

      {run.rankingReasons.length > 0 ? (
        <ul className="ranking-reasons">
          {run.rankingReasons.slice(0, 3).map((reason) => (
            <li key={reason}>{reason}</li>
          ))}
        </ul>
      ) : null}
    </article>
  );
}
