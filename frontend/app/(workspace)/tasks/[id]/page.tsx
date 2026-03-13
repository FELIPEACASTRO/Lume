"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { getTask } from "@/lib/api/workspace";

export default function TaskDetailPage() {
  const params = useParams<{ id: string }>();
  const taskId = typeof params?.id === "string" ? params.id : "";

  const taskQuery = useQuery({
    queryKey: ["task-detail", taskId],
    queryFn: () => getTask(taskId),
    enabled: taskId.length > 0
  });

  if (taskQuery.isLoading) {
    return (
      <div className="screen-center">
        <div style={{ display: "grid", gap: "0.75rem", width: "280px" }}>
          <div className="skeleton-line" style={{ width: "55%", height: "18px" }} />
          <div className="skeleton-line" style={{ width: "100%" }} />
          <div className="skeleton-line" style={{ width: "75%" }} />
        </div>
      </div>
    );
  }

  if (!taskQuery.data) {
    return (
      <EmptyState
        title="Tarefa indisponivel"
        description="Nao foi possivel carregar a tarefa solicitada."
      />
    );
  }

  const detail = taskQuery.data;
  const task = detail.task;

  return (
    <>
      <PageHeader
        title={task.title}
        description={task.summary || task.prompt}
      />

      <div className="field-inline" style={{ flexWrap: "wrap" }}>
        <Link className="button-secondary" href="/tasks">
          Voltar para tarefas
        </Link>
        {task.projectId ? (
          <Link className="button-secondary" href={`/projetos`}>
            Abrir projeto
          </Link>
        ) : null}
      </div>

      <div className="grid-2">
        <Panel title="Resumo operacional" subtitle="Estado real e rastreabilidade da execucao.">
          <div className="item-list">
            <div className="item-row">
              <strong>Status</strong>
              <StatusBadge label={task.statusLabel} variant={toVariant(task.runtimeState)} />
              <p className="muted">{task.runtimeState}</p>
            </div>
            <div className="item-row">
              <strong>Projeto</strong>
              <p>{task.projectName || "Sem projeto vinculado"}</p>
            </div>
            <div className="item-row">
              <strong>Dono</strong>
              <p>{task.ownerName}</p>
            </div>
            <div className="item-row">
              <strong>Ultima atualizacao</strong>
              <p>{task.updatedAt}</p>
            </div>
            <div className="item-row">
              <strong>Runtime IA</strong>
              <p>{task.providerCode ? `${task.providerCode} · ${task.modelCode} · ${task.versionLabel}` : "Nao selecionado"}</p>
            </div>
            {task.lastError ? (
              <div className="item-row">
                <strong>Erro</strong>
                <p>{task.lastError}</p>
              </div>
            ) : null}
          </div>
        </Panel>

        <Panel title="Prompt da tarefa" subtitle="Objetivo original enviado para o runtime.">
          <div className="item-row">
            <p>{task.prompt}</p>
          </div>
        </Panel>
      </div>

      <Panel title="Etapas" subtitle="Historico persistido da execucao da tarefa.">
        {detail.steps.length > 0 ? (
          <div className="item-list">
            {detail.steps.map((step) => (
              <div className="item-row" key={step.id}>
                <strong>
                  {step.stepOrder}. {step.title}
                </strong>
                <p>{step.detail}</p>
                <p className="muted">
                  {step.stepType} · {step.statusLabel}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sem etapas registradas"
            description="Esta tarefa ainda nao possui pipeline detalhado persistido."
          />
        )}
      </Panel>

      {detail.followUpSuggestions.length > 0 ? (
        <Panel title="Proximos passos" subtitle="Sugestoes reais derivadas do fluxo atual.">
          <div className="item-list">
            {detail.followUpSuggestions.map((item) => (
              <div className="item-row" key={item}>
                <p>{item}</p>
              </div>
            ))}
          </div>
        </Panel>
      ) : null}
    </>
  );
}

function toVariant(runtimeState: string): "active" | "attention" | "unavailable" | "restricted" {
  const value = (runtimeState || "").toLowerCase();
  if (value.includes("completed") || value.includes("running")) {
    return "active";
  }
  if (value.includes("failed") || value.includes("error")) {
    return "unavailable";
  }
  if (value.includes("blocked") || value.includes("restricted")) {
    return "restricted";
  }
  return "attention";
}
