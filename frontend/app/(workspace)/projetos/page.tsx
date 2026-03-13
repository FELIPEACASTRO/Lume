"use client";

import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { createProject, listProjects, updateProject } from "@/lib/api/workspace";

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [summary, setSummary] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState("");
  const [editingSummary, setEditingSummary] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: listProjects
  });

  const createProjectMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      setName("");
      setSummary("");
      setFeedback("Projeto criado com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      queryClient.invalidateQueries({ queryKey: ["workspace-onboarding"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao criar projeto.");
    }
  });

  const updateProjectMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: { name: string; summary: string } }) =>
      updateProject(id, payload),
    onSuccess: () => {
      setFeedback("Projeto atualizado com sucesso.");
      setEditingId(null);
      setEditingName("");
      setEditingSummary("");
      queryClient.invalidateQueries({ queryKey: ["projects"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao atualizar projeto.");
    }
  });

  function submitCreateProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim() || !summary.trim()) {
      setFeedback("Preencha nome e resumo para criar o projeto.");
      return;
    }
    createProjectMutation.mutate({
      name: name.trim(),
      summary: summary.trim()
    });
  }

  function submitUpdateProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editingId) {
      return;
    }
    if (!editingName.trim() || !editingSummary.trim()) {
      setFeedback("Preencha nome e resumo para atualizar o projeto.");
      return;
    }
    updateProjectMutation.mutate({
      id: editingId,
      payload: {
        name: editingName.trim(),
        summary: editingSummary.trim()
      }
    });
  }

  return (
    <>
      <PageHeader
        title="Projetos"
        description="Organize contexto, ownership e andamento do trabalho no workspace."
      />
      <Panel title="Novo projeto" subtitle="Crie o primeiro contexto operacional do workspace.">
        <form className="form-stack" onSubmit={submitCreateProject}>
          <label>
            Nome
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Ex.: Operacao comercial"
            />
          </label>
          <label>
            Resumo
            <textarea
              value={summary}
              onChange={(event) => setSummary(event.target.value)}
              placeholder="Objetivo principal e resultado esperado"
            />
          </label>
          <button className="button-primary" type="submit" disabled={createProjectMutation.isPending}>
            {createProjectMutation.isPending ? "Criando..." : "Criar projeto"}
          </button>
        </form>
        {feedback ? <p className="message-info">{feedback}</p> : null}
      </Panel>
      <Panel title="Projetos ativos" subtitle="Lista operacional vinda do backend em tempo real.">
        {projectsQuery.isLoading ? (
          <p className="muted">Carregando projetos...</p>
        ) : projectsQuery.data && projectsQuery.data.length > 0 ? (
          <div className="item-list">
            {projectsQuery.data.map((project) => (
              <div className="item-row" key={project.id}>
                {editingId === project.id ? (
                  <form className="form-stack" onSubmit={submitUpdateProject}>
                    <label>
                      Nome
                      <input
                        value={editingName}
                        onChange={(event) => setEditingName(event.target.value)}
                      />
                    </label>
                    <label>
                      Resumo
                      <textarea
                        value={editingSummary}
                        onChange={(event) => setEditingSummary(event.target.value)}
                      />
                    </label>
                    <div className="field-inline">
                      <button
                        className="button-primary"
                        type="submit"
                        disabled={updateProjectMutation.isPending}
                      >
                        {updateProjectMutation.isPending ? "Salvando..." : "Salvar"}
                      </button>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => {
                          setEditingId(null);
                          setEditingName("");
                          setEditingSummary("");
                        }}
                      >
                        Cancelar
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <strong>{project.name}</strong>
                    <p>{project.summary}</p>
                    <p className="muted">
                      Responsavel: {project.ownerName} · Tarefas: {project.taskCount}
                    </p>
                    <StatusBadge label={project.statusLabel} variant={toVariant(project.availability)} />
                    <div className="field-inline" style={{ marginTop: "0.5rem" }}>
                      <button
                        type="button"
                        className="button-secondary"
                        onClick={() => {
                          setEditingId(project.id);
                          setEditingName(project.name);
                          setEditingSummary(project.summary);
                        }}
                      >
                        Editar
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sem projetos"
            description="Crie o primeiro projeto para estruturar tarefas e contexto."
          />
        )}
      </Panel>
    </>
  );
}

function toVariant(availability: string): "active" | "attention" | "unavailable" | "restricted" {
  const value = (availability || "").toLowerCase();
  if (value.includes("live") || value.includes("active")) {
    return "active";
  }
  if (value.includes("blocked") || value.includes("restricted")) {
    return "restricted";
  }
  if (value.includes("error") || value.includes("unavailable")) {
    return "unavailable";
  }
  return "attention";
}
