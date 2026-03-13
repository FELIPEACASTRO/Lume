"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import {
  createKnowledgeSource,
  getSettingsUiOptions,
  listKnowledgeSources,
  listLibraryEntries,
  listProjects,
  updateKnowledgeSource
} from "@/lib/api/workspace";

export default function FilesPage() {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [sourceUri, setSourceUri] = useState("");
  const [sourceType, setSourceType] = useState("");
  const [projectId, setProjectId] = useState<string>("");
  const [note, setNote] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [editingSourceId, setEditingSourceId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState("");

  const knowledgeQuery = useQuery({
    queryKey: ["knowledge-sources"],
    queryFn: listKnowledgeSources
  });

  const projectsQuery = useQuery({
    queryKey: ["projects"],
    queryFn: listProjects
  });

  const libraryQuery = useQuery({
    queryKey: ["library-entries"],
    queryFn: listLibraryEntries
  });

  const uiOptionsQuery = useQuery({
    queryKey: ["settings-ui-options"],
    queryFn: getSettingsUiOptions
  });

  const sourceTypeOptions = useMemo(
    () => uiOptionsQuery.data?.knowledgeSourceTypes ?? [],
    [uiOptionsQuery.data]
  );

  useEffect(() => {
    if (sourceType || sourceTypeOptions.length === 0) {
      return;
    }
    const defaultOption = sourceTypeOptions.find((item) => item.defaultOption) ?? sourceTypeOptions[0];
    setSourceType(defaultOption.code);
  }, [sourceType, sourceTypeOptions]);

  const createSourceMutation = useMutation({
    mutationFn: createKnowledgeSource,
    onSuccess: () => {
      setTitle("");
      setSourceUri("");
      const defaultOption = sourceTypeOptions.find((item) => item.defaultOption) ?? sourceTypeOptions[0];
      setSourceType(defaultOption?.code ?? "");
      setProjectId("");
      setNote("");
      setFeedback("Fonte criada com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["knowledge-sources"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao criar source.");
    }
  });

  const updateSourceMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Parameters<typeof updateKnowledgeSource>[1] }) =>
      updateKnowledgeSource(id, payload),
    onSuccess: () => {
      setFeedback("Fonte atualizada.");
      setEditingSourceId(null);
      setEditingNote("");
      queryClient.invalidateQueries({ queryKey: ["knowledge-sources"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao atualizar source.");
    }
  });

  function submitCreateSource(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !note.trim() || !sourceType) {
      setFeedback("Titulo e nota sao obrigatorios.");
      return;
    }
    createSourceMutation.mutate({
      title: title.trim(),
      sourceType,
      sourceUri: sourceUri.trim() || undefined,
      projectId: projectId || null,
      note: note.trim(),
      enabledForAgents: true
    });
  }

  return (
    <>
      <PageHeader
        title="Arquivos e conhecimento"
        description="Gerencie fontes, documentos e entregas com rastreabilidade."
      />
      <Panel title="Nova fonte de conhecimento" subtitle="Cadastre fontes para enriquecer contexto do workspace.">
        <form className="form-grid" onSubmit={submitCreateSource}>
          <label>
            Titulo
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Ex.: Politica comercial 2026"
            />
          </label>
          <label>
            Tipo de fonte
            <select value={sourceType} onChange={(event) => setSourceType(event.target.value)}>
              <option value="">Selecione</option>
              {sourceTypeOptions.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="wide">
            URI ou origem
            <input
              value={sourceUri}
              onChange={(event) => setSourceUri(event.target.value)}
              placeholder="https://... ou caminho da fonte"
            />
          </label>
          <label>
            Projeto
            <select value={projectId} onChange={(event) => setProjectId(event.target.value)}>
              <option value="">Nao vincular</option>
              {(projectsQuery.data ?? []).map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nota operacional
            <input
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Resumo de uso da fonte"
            />
          </label>
          <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
            <button
              className="button-primary"
              type="submit"
              disabled={createSourceMutation.isPending || sourceTypeOptions.length === 0 || !sourceType}
            >
              {createSourceMutation.isPending ? "Salvando..." : "Salvar fonte"}
            </button>
          </div>
        </form>
        {feedback ? <p className="message-info">{feedback}</p> : null}
      </Panel>
      <div className="grid-2">
        <Panel title="Knowledge sources" subtitle="Fontes indexadas para uso em agentes e tarefas.">
          {knowledgeQuery.isLoading ? (
            <p className="muted">Carregando fontes...</p>
          ) : knowledgeQuery.data && knowledgeQuery.data.length > 0 ? (
            <div className="item-list">
              {knowledgeQuery.data.map((source) => (
                <div className="item-row" key={source.id}>
                  <strong>{source.title}</strong>
                  <p>{source.sourceUri}</p>
                  <p className="muted">
                    Projeto: {source.projectName || "Nao vinculado"} · Documentos: {source.documentCount}
                  </p>
                  <StatusBadge label={source.statusLabel} variant={toVariant(source.availability)} />
                  {editingSourceId === source.id ? (
                    <form
                      className="form-stack"
                      onSubmit={(event) => {
                        event.preventDefault();
                        if (!editingNote.trim()) {
                          setFeedback("Informe uma nota para atualizar.");
                          return;
                        }
                        updateSourceMutation.mutate({
                          id: source.id,
                          payload: {
                            note: editingNote.trim()
                          }
                        });
                      }}
                    >
                      <label>
                        Nota
                        <input
                          value={editingNote}
                          onChange={(event) => setEditingNote(event.target.value)}
                        />
                      </label>
                      <div className="field-inline">
                        <button className="button-primary" type="submit" disabled={updateSourceMutation.isPending}>
                          {updateSourceMutation.isPending ? "Salvando..." : "Salvar"}
                        </button>
                        <button
                          className="button-secondary"
                          type="button"
                          onClick={() => {
                            setEditingSourceId(null);
                            setEditingNote("");
                          }}
                        >
                          Cancelar
                        </button>
                      </div>
                    </form>
                  ) : (
                    <div className="field-inline" style={{ marginTop: "0.5rem" }}>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => {
                          setEditingSourceId(source.id);
                          setEditingNote(source.note);
                        }}
                      >
                        Editar nota
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="Sem fontes cadastradas"
              description="Adicione uma fonte para usar conhecimento contextual no chat."
            />
          )}
        </Panel>

        <Panel title="Biblioteca" subtitle="Entradas operacionais e versoes de artefatos.">
          {libraryQuery.isLoading ? (
            <p className="muted">Carregando biblioteca...</p>
          ) : libraryQuery.data && libraryQuery.data.length > 0 ? (
            <div className="item-list">
              {libraryQuery.data.map((entry) => (
                <div className="item-row" key={entry.id}>
                  <strong>{entry.title}</strong>
                  <p>{entry.summary}</p>
                  <p className="muted">
                    Tipo: {entry.entryType} · Versoes: {entry.versionCount}
                  </p>
                  <StatusBadge label={entry.status} variant={toVariant(entry.availability)} />
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="Biblioteca vazia"
              description="Os arquivos e entregas publicados no workspace aparecem aqui."
            />
          )}
        </Panel>
      </div>
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
