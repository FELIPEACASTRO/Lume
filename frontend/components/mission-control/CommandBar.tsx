"use client";

import { FormEvent } from "react";
import { MultiInferenceCostSummary } from "@/lib/api/types";

type Props = {
  prompt: string;
  rankingPolicy: string;
  routingPolicy: string;
  disabled?: boolean;
  isRunning?: boolean;
  costPreview?: MultiInferenceCostSummary | null;
  onPromptChange: (value: string) => void;
  onRankingPolicyChange: (value: string) => void;
  onRoutingPolicyChange: (value: string) => void;
  onRun: () => void;
};

export function CommandBar({
  prompt,
  rankingPolicy,
  routingPolicy,
  disabled,
  isRunning,
  costPreview,
  onPromptChange,
  onRankingPolicyChange,
  onRoutingPolicyChange,
  onRun
}: Props) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (disabled || !prompt.trim()) return;
    onRun();
  }

  return (
    <form className="command-bar" onSubmit={submit}>
      <label className="command-bar-query">
        Pergunta / instrução
        <input
          value={prompt}
          onChange={(event) => onPromptChange(event.target.value)}
          placeholder="Digite a solicitação que será enviada para todos os modelos ativos"
        />
      </label>

      <label>
        Ranking
        <select value={rankingPolicy} onChange={(event) => onRankingPolicyChange(event.target.value)}>
          <option value="quality-first">quality-first</option>
          <option value="cost-first">cost-first</option>
          <option value="latency-first">latency-first</option>
        </select>
      </label>

      <label>
        Routing
        <select value={routingPolicy} onChange={(event) => onRoutingPolicyChange(event.target.value)}>
          <option value="balanced">balanced</option>
          <option value="cost-first">cost-first</option>
          <option value="latency-first">latency-first</option>
          <option value="quality-first">quality-first</option>
        </select>
      </label>

      <div className="command-bar-cost">
        <span>Prévia de custo</span>
        <strong>
          {costPreview?.totalEstimatedCostUsd == null
            ? "n/d"
            : `US$ ${costPreview.totalEstimatedCostUsd.toFixed(4)}`}
        </strong>
      </div>

      <button type="submit" className="button-primary command-run-button" disabled={disabled || isRunning || !prompt.trim()}>
        {isRunning ? "Executando..." : "Run"}
      </button>
    </form>
  );
}
