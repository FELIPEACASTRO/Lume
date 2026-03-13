"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { listProjects, listTasks } from "@/lib/api/workspace";

type RuntimeFilter = "all" | "queued" | "running" | "completed" | "failed";

export default function TasksPage() {
  const searchParams = useSearchParams();
  const [runtimeFilter, setRuntimeFilter] = useState<RuntimeFilter>("all");
  const [projectFilter, setProjectFilter] = useState(searchParams.get("project") ?? "");

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: listProjects
  });

  const tasksQuery = useQuery({
    queryKey: ["tasks", projectFilter],
    queryFn: () => listTasks(projectFilter || undefined)
  });

  const visibleTasks = useMemo(() => {
    const tasks = tasksQuery.data ?? [];
    if (runtimeFilter === "all") {
      return tasks;
    }
    return tasks.filter((task) => (task.runtimeState || "").toLowerCase().includes(runtimeFilter));
  }, [tasksQuery.data, runtimeFilter]);

  return (
    <>
      <PageHeader
        title="Tarefas"
        description="Fila operacional do workspace com estado real, dono, runtime e erros."
      />

      <Panel title="Filtros" subtitle="Refine por projeto e estado de execucao.">
        <div className="field-inline" style={{ flexWrap: "wrap" }}>
          <label style={{ minWidth: "16rem" }}>
            Projeto
            <select value={projectFilter} onChange={(event) => setProjectFilter(event.target.value)}>
              <option value="">Todos os projetos</option>
              {(projectsQuery.data ?? []).map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>
          <div className="tab-set" role="tablist" aria-label="Filtro de estado">
            {[
              ["all", "Todas"],
              ["queued", "Na fila"],
              ["running", "Em andamento"],
              ["completed", "Concluidas"],
              ["failed", "Com erro"]
            ].map(([value, label]) => (
              <button
                key={value}
                type="button"
                className={runtimeFilter === value ? "active" : ""}
                onClick={() => setRuntimeFilter(value as RuntimeFilter)}
              >
                {label}
              </button>
            ))}
          </div>
          <Link className="button-secondary" href="/home">
            Nova tarefa
          </Link>
        </div>
      </Panel>

      <Panel title="Trabalho em curso" subtitle="Cada linha reflete o estado persistido da tarefa.">
        {tasksQuery.isLoading ? (
          <p className="muted">Carregando tarefas...</p>
        ) : visibleTasks.length > 0 ? (
          <div className="item-list">
            {visibleTasks.map((task) => (
              <Link className="item-row item-row-link" href={`/tasks/${task.id}`} key={task.id}>
                <strong>{task.title}</strong>
                <p>{task.summary || task.prompt}</p>
                <p className="muted">
                  {task.projectName || "Sem projeto"} · {task.ownerName} · {task.updatedAt}
                </p>
                <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                  <StatusBadge label={task.statusLabel} variant={toVariant(task.runtimeState)} />
                  {task.providerCode ? (
                    <span className="muted">
                      {task.providerCode} · {task.modelCode || "modelo nao informado"}
                    </span>
                  ) : (
                    <span className="muted">Sem runtime IA selecionado</span>
                  )}
                </div>
                {task.lastError ? <p className="message-error">{task.lastError}</p> : null}
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sem tarefas"
            description="Crie a primeira tarefa para iniciar a operacao deste workspace."
          />
        )}
      </Panel>
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
