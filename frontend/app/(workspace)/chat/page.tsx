"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { CommandBar } from "@/components/mission-control/CommandBar";
import { CostMeter } from "@/components/mission-control/CostMeter";
import { ExecutionSetPanel } from "@/components/mission-control/ExecutionSetPanel";
import { RankedResultCard } from "@/components/mission-control/RankedResultCard";
import { listModels, listProviderStatus } from "@/lib/api/providers";
import { getSession } from "@/lib/api/auth";
import { executeMultiInference } from "@/lib/api/inference";
import { ExecutionSetTarget, ModelResponse, MultiInferenceResult } from "@/lib/api/types";

const LOCAL_STORAGE_KEY_PREFIX = "lume:mission-control:execution-set:";

export default function ChatPage() {
  return (
    <Suspense fallback={<div className="screen-center"><span className="muted">Carregando Mission Control...</span></div>}>
      <ChatMissionControlPage />
    </Suspense>
  );
}

function ChatMissionControlPage() {
  const [prompt, setPrompt] = useState("");
  const [executionSet, setExecutionSet] = useState<ExecutionSetTarget[]>([]);
  const [result, setResult] = useState<MultiInferenceResult | null>(null);
  const [rankingPolicy, setRankingPolicy] = useState("quality-first");
  const [routingPolicy, setRoutingPolicy] = useState("balanced");
  const [modelsByProvider, setModelsByProvider] = useState<Record<string, ModelResponse[]>>({});
  const [collapsedCards, setCollapsedCards] = useState<Record<string, boolean>>({});
  const [feedback, setFeedback] = useState<string | null>(null);

  const sessionQuery = useQuery({
    queryKey: ["session"],
    queryFn: getSession
  });

  const providersQuery = useQuery({
    queryKey: ["mission-control-providers"],
    queryFn: listProviderStatus
  });

  const workspaceStorageKey = useMemo(() => {
    const workspaceId = sessionQuery.data?.workspace?.id;
    return `${LOCAL_STORAGE_KEY_PREFIX}${workspaceId ?? "default"}`;
  }, [sessionQuery.data?.workspace?.id]);

  useEffect(() => {
    const providers = providersQuery.data ?? [];
    const eligibleProviders = providers.filter(
      (provider) =>
        provider.executionSupported &&
        provider.configured &&
        provider.category === "text-runtime"
    );
    if (eligibleProviders.length === 0) {
      setModelsByProvider({});
      return;
    }

    let cancelled = false;
    Promise.all(
      eligibleProviders.map(async (provider) => {
        const models = await listModels(provider.providerCode);
        return [provider.providerCode, models] as const;
      })
    ).then((entries) => {
      if (cancelled) return;
      setModelsByProvider(Object.fromEntries(entries));
    }).catch(() => {
      if (cancelled) return;
      setModelsByProvider({});
    });

    return () => {
      cancelled = true;
    };
  }, [providersQuery.data]);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(workspaceStorageKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as ExecutionSetTarget[];
      if (Array.isArray(parsed)) {
        setExecutionSet(parsed.filter((item) => !!item?.providerCode && !!item?.modelCode));
      }
    } catch {
      setExecutionSet([]);
    }
  }, [workspaceStorageKey]);

  useEffect(() => {
    window.localStorage.setItem(workspaceStorageKey, JSON.stringify(executionSet));
  }, [executionSet, workspaceStorageKey]);

  const runMutation = useMutation({
    mutationFn: executeMultiInference,
    onSuccess: (payload) => {
      setResult(payload);
      setFeedback(null);
      setCollapsedCards({});
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao executar multi-modelo.");
      setResult(null);
    }
  });

  function addTarget(target: ExecutionSetTarget) {
    setExecutionSet((current) => {
      const key = `${target.providerCode}:${target.modelCode}`;
      if (current.some((entry) => `${entry.providerCode}:${entry.modelCode}` === key)) {
        return current;
      }
      return [...current, target];
    });
  }

  function removeTarget(target: ExecutionSetTarget) {
    setExecutionSet((current) =>
      current.filter((entry) => !(entry.providerCode === target.providerCode && entry.modelCode === target.modelCode))
    );
  }

  function runExecution() {
    const text = prompt.trim();
    if (!text) {
      setFeedback("Digite uma pergunta antes de executar.");
      return;
    }
    if (executionSet.length === 0) {
      setFeedback("Nenhum modelo ativo no execution set. Adicione pelo menos um.");
      return;
    }

    setFeedback(null);
    runMutation.mutate({
      executionSet,
      prompt: text,
      rankingPolicy: rankingPolicy as "quality-first" | "cost-first" | "latency-first",
      routingPolicy: routingPolicy as "balanced" | "cost-first" | "latency-first" | "quality-first",
      workspaceId: sessionQuery.data?.workspace?.id?.toString()
    });
  }

  const winner = result?.winner ?? null;
  const comparatorRuns = useMemo(() => {
    if (!result?.runs) return [];
    return result.runs.filter((run) => !(winner && run.providerCode === winner.providerCode && run.modelCode === winner.modelCode));
  }, [result?.runs, winner]);

  return (
    <div className="workspace-content mission-content">
      <CommandBar
        prompt={prompt}
        rankingPolicy={rankingPolicy}
        routingPolicy={routingPolicy}
        disabled={executionSet.length === 0}
        isRunning={runMutation.isPending}
        costPreview={result?.costSummary}
        onPromptChange={setPrompt}
        onRankingPolicyChange={setRankingPolicy}
        onRoutingPolicyChange={setRoutingPolicy}
        onRun={runExecution}
      />

      {feedback ? <p className="message-error">{feedback}</p> : null}

      <div className="mission-grid">
        <ExecutionSetPanel
          providers={providersQuery.data ?? []}
          modelsByProvider={modelsByProvider}
          executionSet={executionSet}
          onAddTarget={addTarget}
          onRemoveTarget={removeTarget}
        />

        <section className="result-arena">
          <header className="result-arena-header">
            <h2>Result Arena</h2>
            <p>Ranking único orientado por política com rastreabilidade operacional.</p>
          </header>

          {result?.costSummary ? (
            <CostMeter
              totalEstimatedCostUsd={result.costSummary.totalEstimatedCostUsd}
              completedRuns={result.costSummary.completedRuns}
              failedRuns={result.costSummary.failedRuns}
            />
          ) : null}

          {winner ? (
            <RankedResultCard run={winner} rank={1} winner />
          ) : (
            <div className="empty-state">
              <strong>Aguardando execução</strong>
              <p>Envie uma pergunta para ranquear os modelos ativos do execution set.</p>
            </div>
          )}

          {comparatorRuns.length > 0 ? (
            <div className="comparison-panel">
              <h3>Comparativo secundário</h3>
              {comparatorRuns.map((run, index) => {
                const key = `${run.providerCode}:${run.modelCode}`;
                const collapsed = collapsedCards[key] ?? true;
                return (
                  <RankedResultCard
                    key={key}
                    run={run}
                    rank={index + 2}
                    collapsed={collapsed}
                    onToggleCollapsed={() =>
                      setCollapsedCards((current) => ({ ...current, [key]: !collapsed }))
                    }
                  />
                );
              })}
            </div>
          ) : null}

          {result?.policyDecisionSummary ? (
            <p className="message-info">{result.policyDecisionSummary}</p>
          ) : null}
        </section>
      </div>
    </div>
  );
}
