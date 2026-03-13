"use client";

import { FormEvent } from "react";
import Link from "next/link";
import { Panel } from "@/components/ui/Panel";
import { OnboardingActivationStatus, UiOptionItem, WorkspaceOnboarding } from "@/lib/api/types";

type Props = {
  onboarding: WorkspaceOnboarding;
  onboardingStatus: OnboardingActivationStatus;
  onboardingPrimaryUseCase: string;
  onboardingWorkStyle: string;
  onboardingUseCaseOptions: UiOptionItem[];
  onboardingWorkStyleOptions: UiOptionItem[];
  isSaving: boolean;
  onPrimaryUseCaseChange: (value: string) => void;
  onWorkStyleChange: (value: string) => void;
  onSubmitProfile: (event: FormEvent<HTMLFormElement>) => void;
  onStartNewTask: () => void;
};

export function HomeOnboardingPanel({
  onboarding,
  onboardingStatus,
  onboardingPrimaryUseCase,
  onboardingWorkStyle,
  onboardingUseCaseOptions,
  onboardingWorkStyleOptions,
  isSaving,
  onPrimaryUseCaseChange,
  onWorkStyleChange,
  onSubmitProfile,
  onStartNewTask
}: Props) {
  return (
    <Panel title="Onboarding guiado" subtitle="Conclua os passos para ativar o workspace em modo operacional.">
      <div className="item-list">
        <div className="item-row">
          <strong>Etapa atual</strong>
          <p>{friendlyOnboardingStep(onboardingStatus)}</p>
          <p className="muted">{onboarding.activationNote}</p>
        </div>
      </div>

      {onboardingStatus === "started" ? (
        <form className="form-grid" onSubmit={onSubmitProfile}>
          <label>
            Objetivo principal
            <select
              value={onboardingPrimaryUseCase}
              onChange={(event) => onPrimaryUseCaseChange(event.target.value)}
            >
              <option value="">Selecione</option>
              {onboardingUseCaseOptions.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Estilo de trabalho
            <select
              value={onboardingWorkStyle}
              onChange={(event) => onWorkStyleChange(event.target.value)}
            >
              <option value="">Selecione</option>
              {onboardingWorkStyleOptions.map((option) => (
                <option key={option.code} value={option.code}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
            <button
              className="button-primary"
              type="submit"
              disabled={
                isSaving ||
                onboardingUseCaseOptions.length === 0 ||
                onboardingWorkStyleOptions.length === 0
              }
            >
              {isSaving ? "Salvando..." : "Salvar perfil inicial"}
            </button>
          </div>
        </form>
      ) : (
        <div className="field-inline">
          {onboardingStatus === "profile_selected" ? (
            <Link className="button-primary" href="/projetos">
              Criar primeiro projeto
            </Link>
          ) : null}
          {onboardingStatus === "first_project_created" ? (
            <button className="button-primary" type="button" onClick={onStartNewTask}>
              Criar primeira tarefa
            </button>
          ) : null}
          {onboardingStatus === "first_task_created" ? (
            <Link className="button-primary" href="/prompts">
              Salvar primeiro template
            </Link>
          ) : null}
          {onboardingStatus === "first_prompt_sent" ? (
            <Link className="button-primary" href="/chat">
              Abrir chat e concluir ativacao
            </Link>
          ) : null}
        </div>
      )}
    </Panel>
  );
}

function friendlyOnboardingStep(status: OnboardingActivationStatus): string {
  switch (status) {
    case "started":
      return "Iniciado";
    case "profile_selected":
      return "Perfil definido";
    case "first_project_created":
      return "Primeiro projeto criado";
    case "first_task_created":
      return "Primeira tarefa criada";
    case "first_prompt_sent":
      return "Primeiro template salvo";
    case "completed":
      return "Concluido";
    default:
      return "Iniciado";
  }
}
