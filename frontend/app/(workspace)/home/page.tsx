"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { HomeOnboardingPanel } from "@/components/home/HomeOnboardingPanel";
import { HomeCommandCard } from "@/components/home/HomeCommandCard";
import { HomeOverviewPanels } from "@/components/home/HomeOverviewPanels";
import {
  createTask,
  getCurrentOnboarding,
  getHomeOverview,
  getSettingsUiOptions,
  getShellNavigation,
  searchResults,
  updateCurrentOnboarding
} from "@/lib/api/workspace";
import { listModels, listProviderStatus } from "@/lib/api/providers";
import { OnboardingActivationStatus } from "@/lib/api/types";

type CommandMode = "buscar" | "nova_tarefa";

export default function HomePage() {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<CommandMode>("buscar");
  const [query, setQuery] = useState("");
  const [taskPrompt, setTaskPrompt] = useState("");
  const [searchResult, setSearchResult] = useState<Awaited<ReturnType<typeof searchResults>> | null>(null);
  const [selectedTaskType, setSelectedTaskType] = useState("");
  const [selectedProvider, setSelectedProvider] = useState("");
  const [selectedModel, setSelectedModel] = useState("");
  const [versionLabel, setVersionLabel] = useState("");
  const [commandError, setCommandError] = useState<string | null>(null);
  const [onboardingPrimaryUseCase, setOnboardingPrimaryUseCase] = useState("");
  const [onboardingWorkStyle, setOnboardingWorkStyle] = useState("");

  const homeOverviewQuery = useQuery({ queryKey: ["home-overview"], queryFn: getHomeOverview });
  const onboardingQuery = useQuery({ queryKey: ["workspace-onboarding"], queryFn: getCurrentOnboarding });
  const navigationQuery = useQuery({ queryKey: ["shell-navigation"], queryFn: getShellNavigation });
  const uiOptionsQuery = useQuery({ queryKey: ["settings-ui-options"], queryFn: getSettingsUiOptions });
  const providerStatusQuery = useQuery({ queryKey: ["provider-status"], queryFn: listProviderStatus });

  const providerModelsQuery = useQuery({
    queryKey: ["provider-models", selectedProvider],
    queryFn: () => listModels(selectedProvider),
    enabled: selectedProvider.length > 0
  });

  const searchableTaskTypes = useMemo(() => navigationQuery.data?.taskTypes ?? [], [navigationQuery.data]);
  const onboardingUseCaseOptions = useMemo(() => uiOptionsQuery.data?.onboardingPrimaryUseCases ?? [], [uiOptionsQuery.data]);
  const onboardingWorkStyleOptions = useMemo(() => uiOptionsQuery.data?.onboardingWorkStyles ?? [], [uiOptionsQuery.data]);

  const taskReadyProviders = useMemo(() => {
    return (providerStatusQuery.data ?? []).filter(
      (provider) => provider.executionSupported && provider.configured && provider.category === "text-runtime"
    );
  }, [providerStatusQuery.data]);

  const modelsForTasks = useMemo(() => {
    return (providerModelsQuery.data ?? []).filter((model) => model.enabledForAgents);
  }, [providerModelsQuery.data]);

  const searchMutation = useMutation({
    mutationFn: searchResults,
    onSuccess: (payload) => { setSearchResult(payload); setCommandError(null); },
    onError: (error) => { setCommandError(error instanceof Error ? error.message : "Falha ao executar a busca."); }
  });

  const createTaskMutation = useMutation({
    mutationFn: createTask,
    onSuccess: (task) => {
      setTaskPrompt(""); setCommandError(null); setSearchResult(null);
      setQuery(""); setMode("buscar");
      setCommandError(`Tarefa criada com sucesso: ${task.task.title}`);
      queryClient.invalidateQueries({ queryKey: ["workspace-onboarding"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => { setCommandError(error instanceof Error ? error.message : "Falha ao criar tarefa."); }
  });

  const onboardingMutation = useMutation({
    mutationFn: updateCurrentOnboarding,
    onSuccess: (data) => {
      setOnboardingPrimaryUseCase(data.primaryUseCase);
      setOnboardingWorkStyle(data.workStyle);
      queryClient.invalidateQueries({ queryKey: ["workspace-onboarding"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => { setCommandError(error instanceof Error ? error.message : "Falha ao atualizar onboarding."); }
  });

  function handleProviderChange(providerCode: string) {
    setSelectedProvider(providerCode);
    setSelectedModel("");
    setVersionLabel("");
  }

  function handleModelChange(modelCode: string) {
    setSelectedModel(modelCode);
    const model = modelsForTasks.find((item) => item.code === modelCode);
    setVersionLabel(model?.versionLabel ?? "");
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!query.trim()) { setCommandError("Digite um termo para buscar."); return; }
    searchMutation.mutate(query.trim());
  }

  function handleTaskSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!taskPrompt.trim()) { setCommandError("Descreva o objetivo da tarefa."); return; }
    if (!selectedTaskType) { setCommandError("Selecione o tipo de tarefa."); return; }
    if (!selectedProvider || !selectedModel || !versionLabel) {
      setCommandError("Selecione um provedor e modelo de execucao IA."); return;
    }
    createTaskMutation.mutate({
      prompt: taskPrompt.trim(), taskType: selectedTaskType,
      providerCode: selectedProvider, modelCode: selectedModel, versionLabel
    });
  }

  const onboarding = onboardingQuery.data;
  const onboardingStatus: OnboardingActivationStatus = onboarding?.activationStatus ?? "started";
  const onboardingCompleted = onboardingStatus === "completed";

  useEffect(() => {
    if (!onboarding) return;
    setOnboardingPrimaryUseCase(onboarding.primaryUseCase);
    setOnboardingWorkStyle(onboarding.workStyle);
  }, [onboarding]);

  useEffect(() => {
    if (onboardingPrimaryUseCase || onboardingUseCaseOptions.length === 0) return;
    const defaultOption = onboardingUseCaseOptions.find((item) => item.defaultOption) ?? onboardingUseCaseOptions[0];
    setOnboardingPrimaryUseCase(defaultOption.code);
  }, [onboardingPrimaryUseCase, onboardingUseCaseOptions]);

  useEffect(() => {
    if (onboardingWorkStyle || onboardingWorkStyleOptions.length === 0) return;
    const defaultOption = onboardingWorkStyleOptions.find((item) => item.defaultOption) ?? onboardingWorkStyleOptions[0];
    setOnboardingWorkStyle(defaultOption.code);
  }, [onboardingWorkStyle, onboardingWorkStyleOptions]);

  function submitOnboardingProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!onboardingPrimaryUseCase || !onboardingWorkStyle) {
      setCommandError("Catalogo de onboarding indisponivel para este workspace."); return;
    }
    onboardingMutation.mutate({
      primaryUseCase: onboardingPrimaryUseCase, workStyle: onboardingWorkStyle,
      activationStatus: "profile_selected"
    });
  }

  if (homeOverviewQuery.isLoading || navigationQuery.isLoading || onboardingQuery.isLoading || uiOptionsQuery.isLoading) {
    return (
      <div className="screen-center">
        <div style={{ display: "grid", gap: "0.75rem", width: "280px" }}>
          <div className="skeleton-line" style={{ width: "50%", height: "18px" }} />
          <div className="skeleton-line" style={{ width: "100%" }} />
          <div className="skeleton-line" style={{ width: "85%" }} />
          <div className="skeleton-line" style={{ width: "60%" }} />
        </div>
      </div>
    );
  }

  const overview = homeOverviewQuery.data;
  if (!overview) {
    return <div className="screen-center">Nao foi possivel carregar o painel.</div>;
  }

  return (
    <>
      <PageHeader title={overview.headline} description={overview.supportingText} />

      {!onboardingCompleted && onboarding ? (
        <HomeOnboardingPanel
          onboarding={onboarding}
          onboardingStatus={onboardingStatus}
          onboardingPrimaryUseCase={onboardingPrimaryUseCase}
          onboardingWorkStyle={onboardingWorkStyle}
          onboardingUseCaseOptions={onboardingUseCaseOptions}
          onboardingWorkStyleOptions={onboardingWorkStyleOptions}
          isSaving={onboardingMutation.isPending}
          onPrimaryUseCaseChange={setOnboardingPrimaryUseCase}
          onWorkStyleChange={setOnboardingWorkStyle}
          onSubmitProfile={submitOnboardingProfile}
          onStartNewTask={() => setMode("nova_tarefa")}
        />
      ) : null}

      <HomeCommandCard
        mode={mode}
        query={query}
        taskPrompt={taskPrompt}
        selectedTaskType={selectedTaskType}
        selectedProvider={selectedProvider}
        selectedModel={selectedModel}
        versionLabel={versionLabel}
        searchableTaskTypes={searchableTaskTypes}
        taskReadyProviders={taskReadyProviders}
        modelsForTasks={modelsForTasks}
        isSearching={searchMutation.isPending}
        isCreatingTask={createTaskMutation.isPending}
        commandError={commandError}
        onModeChange={setMode}
        onQueryChange={setQuery}
        onTaskPromptChange={setTaskPrompt}
        onTaskTypeChange={setSelectedTaskType}
        onProviderChange={handleProviderChange}
        onModelChange={handleModelChange}
        onSearchSubmit={handleSearchSubmit}
        onTaskSubmit={handleTaskSubmit}
      />

      <HomeOverviewPanels
        overview={overview}
        searchResult={searchResult}
        isSearchMode={mode === "buscar"}
      />
    </>
  );
}
