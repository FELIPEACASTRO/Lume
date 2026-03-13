"use client";

import { FormEvent } from "react";
import { ModelResponse, ProviderStatus, ShellNavigationResponse } from "@/lib/api/types";

type CommandMode = "buscar" | "nova_tarefa";

type Props = {
  mode: CommandMode;
  query: string;
  taskPrompt: string;
  selectedTaskType: string;
  selectedProvider: string;
  selectedModel: string;
  versionLabel: string;
  searchableTaskTypes: ShellNavigationResponse["taskTypes"];
  taskReadyProviders: ProviderStatus[];
  modelsForTasks: ModelResponse[];
  isSearching: boolean;
  isCreatingTask: boolean;
  commandError: string | null;
  onModeChange: (mode: CommandMode) => void;
  onQueryChange: (value: string) => void;
  onTaskPromptChange: (value: string) => void;
  onTaskTypeChange: (value: string) => void;
  onProviderChange: (value: string) => void;
  onModelChange: (value: string) => void;
  onSearchSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onTaskSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function HomeCommandCard({
  mode,
  query,
  taskPrompt,
  selectedTaskType,
  selectedProvider,
  selectedModel,
  versionLabel,
  searchableTaskTypes,
  taskReadyProviders,
  modelsForTasks,
  isSearching,
  isCreatingTask,
  commandError,
  onModeChange,
  onQueryChange,
  onTaskPromptChange,
  onTaskTypeChange,
  onProviderChange,
  onModelChange,
  onSearchSubmit,
  onTaskSubmit
}: Props) {
  return (
    <section className="command-card">
      <div className="tab-set">
        <button
          type="button"
          className={mode === "buscar" ? "active" : ""}
          onClick={() => onModeChange("buscar")}
        >
          Buscar
        </button>
        <button
          type="button"
          className={mode === "nova_tarefa" ? "active" : ""}
          onClick={() => onModeChange("nova_tarefa")}
        >
          Nova tarefa
        </button>
      </div>

      {mode === "buscar" ? (
        <form className="form-stack" onSubmit={onSearchSubmit}>
          <label>
            Busque por tarefa, projeto, pessoa, arquivo ou contexto
            <input
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="Digite sua consulta"
            />
          </label>
          <button className="button-primary" type="submit" disabled={isSearching}>
            {isSearching ? "Buscando..." : "Buscar agora"}
          </button>
        </form>
      ) : (
        <form className="form-stack" onSubmit={onTaskSubmit}>
          <label>
            Objetivo da tarefa
            <textarea
              value={taskPrompt}
              onChange={(event) => onTaskPromptChange(event.target.value)}
              placeholder="Descreva o que precisa ser entregue"
            />
          </label>
          <div className="grid-2">
            <label>
              Tipo da tarefa
              <select
                value={selectedTaskType}
                onChange={(event) => onTaskTypeChange(event.target.value)}
              >
                <option value="">Selecione</option>
                {searchableTaskTypes.map((type) => (
                  <option key={type.taskType} value={type.taskType}>
                    {type.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Provedor
              <select
                value={selectedProvider}
                onChange={(event) => onProviderChange(event.target.value)}
              >
                <option value="">Selecione</option>
                {taskReadyProviders.map((provider) => (
                  <option key={provider.providerCode} value={provider.providerCode}>
                    {provider.providerName}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Modelo
              <select
                value={selectedModel}
                onChange={(event) => onModelChange(event.target.value)}
                disabled={!selectedProvider}
              >
                <option value="">Selecione</option>
                {modelsForTasks.map((model) => (
                  <option key={model.code} value={model.code}>
                    {model.label} · {model.versionLabel}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Versao
              <input value={versionLabel} readOnly />
            </label>
          </div>
          <button
            className="button-primary"
            type="submit"
            disabled={
              isCreatingTask ||
              !selectedProvider ||
              !selectedModel ||
              !versionLabel
            }
          >
            {isCreatingTask ? "Criando..." : "Criar tarefa"}
          </button>
        </form>
      )}

      {commandError ? <p className="message-info">{commandError}</p> : null}
    </section>
  );
}
