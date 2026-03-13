"use client";

import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import {
  createPromptTemplate,
  deletePromptTemplate,
  listPromptTemplates,
  touchPromptTemplate,
  updatePromptTemplate
} from "@/lib/api/workspace";
import { PromptTemplate } from "@/lib/api/types";

export default function PromptsPage() {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [promptBody, setPromptBody] = useState("");
  const [variables, setVariables] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [editingTemplateId, setEditingTemplateId] = useState<string | null>(null);
  const [editingSummary, setEditingSummary] = useState("");
  const [editingPromptBody, setEditingPromptBody] = useState("");
  const [editingVariables, setEditingVariables] = useState("");

  const templatesQuery = useQuery({
    queryKey: ["prompt-templates"],
    queryFn: listPromptTemplates
  });

  const createMutation = useMutation({
    mutationFn: createPromptTemplate,
    onSuccess: () => {
      setTitle("");
      setSummary("");
      setPromptBody("");
      setVariables("");
      setFeedback("Template criado com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["prompt-templates"] });
      queryClient.invalidateQueries({ queryKey: ["workspace-onboarding"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao criar template.");
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Parameters<typeof updatePromptTemplate>[1] }) =>
      updatePromptTemplate(id, payload),
    onSuccess: () => {
      setEditingTemplateId(null);
      setEditingSummary("");
      setEditingPromptBody("");
      setEditingVariables("");
      setFeedback("Template atualizado.");
      queryClient.invalidateQueries({ queryKey: ["prompt-templates"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao atualizar template.");
    }
  });

  const touchMutation = useMutation({
    mutationFn: touchPromptTemplate,
    onSuccess: () => {
      setFeedback("Uso do template registrado.");
      queryClient.invalidateQueries({ queryKey: ["prompt-templates"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao registrar uso do template.");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: deletePromptTemplate,
    onSuccess: () => {
      setFeedback("Template removido.");
      queryClient.invalidateQueries({ queryKey: ["prompt-templates"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao remover template.");
    }
  });

  const parsedVariables = useMemo(
    () =>
      variables
        .split(",")
        .map((value) => value.trim())
        .filter((value) => value.length > 0),
    [variables]
  );

  const parsedEditingVariables = useMemo(
    () =>
      editingVariables
        .split(",")
        .map((value) => value.trim())
        .filter((value) => value.length > 0),
    [editingVariables]
  );

  function submitCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !summary.trim() || !promptBody.trim()) {
      setFeedback("Preencha titulo, resumo e corpo do prompt.");
      return;
    }
    createMutation.mutate({
      title: title.trim(),
      summary: summary.trim(),
      promptBody: promptBody.trim(),
      variables: parsedVariables,
      templateScope: "workspace",
      favorited: false
    });
  }

  function toggleFavorite(template: PromptTemplate) {
    updateMutation.mutate({
      id: template.id,
      payload: {
        favorited: !template.favorited
      }
    });
  }

  function startEdit(template: PromptTemplate) {
    setEditingTemplateId(template.id);
    setEditingSummary(template.summary);
    setEditingPromptBody(template.promptBody);
    setEditingVariables(template.variables.join(", "));
  }

  return (
    <>
      <PageHeader
        title="Prompts e templates"
        description="Biblioteca reutilizavel para acelerar entregas com consistencia."
      />
      <Panel title="Novo template" subtitle="Registre prompts padrao e reutilize no workspace.">
        <form className="form-stack" onSubmit={submitCreate}>
          <label>
            Titulo
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Ex.: Resumo executivo semanal"
            />
          </label>
          <label>
            Resumo
            <input
              value={summary}
              onChange={(event) => setSummary(event.target.value)}
              placeholder="Quando usar e qual resultado esperar"
            />
          </label>
          <label>
            Corpo do prompt
            <textarea
              value={promptBody}
              onChange={(event) => setPromptBody(event.target.value)}
              placeholder="Escreva o template com variaveis como {{contexto}}"
            />
          </label>
          <label>
            Variaveis (separadas por virgula)
            <input
              value={variables}
              onChange={(event) => setVariables(event.target.value)}
              placeholder="contexto, objetivo, publico"
            />
          </label>
          <button className="button-primary" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Salvando..." : "Salvar template"}
          </button>
        </form>
        {feedback ? <p className="message-info">{feedback}</p> : null}
      </Panel>
      <Panel title="Templates do workspace" subtitle="Use, reaproveite e padronize as melhores instrucoes.">
        {templatesQuery.isLoading ? (
          <p className="muted">Carregando templates...</p>
        ) : templatesQuery.data && templatesQuery.data.length > 0 ? (
          <div className="item-list">
            {templatesQuery.data.map((template) => (
              <div className="item-row" key={template.id}>
                {editingTemplateId === template.id ? (
                  <form
                    className="form-stack"
                    onSubmit={(event) => {
                      event.preventDefault();
                      if (!editingSummary.trim() || !editingPromptBody.trim()) {
                        setFeedback("Resumo e corpo do prompt sao obrigatorios.");
                        return;
                      }
                      updateMutation.mutate({
                        id: template.id,
                        payload: {
                          summary: editingSummary.trim(),
                          promptBody: editingPromptBody.trim(),
                          variables: parsedEditingVariables
                        }
                      });
                    }}
                  >
                    <strong>{template.title}</strong>
                    <label>
                      Resumo
                      <input
                        value={editingSummary}
                        onChange={(event) => setEditingSummary(event.target.value)}
                      />
                    </label>
                    <label>
                      Corpo do prompt
                      <textarea
                        value={editingPromptBody}
                        onChange={(event) => setEditingPromptBody(event.target.value)}
                      />
                    </label>
                    <label>
                      Variaveis
                      <input
                        value={editingVariables}
                        onChange={(event) => setEditingVariables(event.target.value)}
                      />
                    </label>
                    <div className="field-inline">
                      <button className="button-primary" type="submit" disabled={updateMutation.isPending}>
                        {updateMutation.isPending ? "Salvando..." : "Salvar alteracoes"}
                      </button>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => {
                          setEditingTemplateId(null);
                          setEditingSummary("");
                          setEditingPromptBody("");
                          setEditingVariables("");
                        }}
                      >
                        Cancelar
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <strong>{template.title}</strong>
                    <p>{template.summary}</p>
                    <p className="muted">
                      Escopo: {template.templateScope} · Variaveis: {template.variables.length}
                    </p>
                    <StatusBadge label={template.statusLabel} variant={toVariant(template.availability)} />
                    <div className="field-inline" style={{ marginTop: "0.5rem" }}>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => toggleFavorite(template)}
                        disabled={updateMutation.isPending}
                      >
                        {template.favorited ? "Desfavoritar" : "Favoritar"}
                      </button>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => touchMutation.mutate(template.id)}
                        disabled={touchMutation.isPending}
                      >
                        Marcar uso
                      </button>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => startEdit(template)}
                      >
                        Editar
                      </button>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => deleteMutation.mutate(template.id)}
                        disabled={deleteMutation.isPending}
                      >
                        Remover
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sem templates"
            description="Crie templates para reduzir retrabalho e padronizar o time."
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
