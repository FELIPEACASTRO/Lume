import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FiArrowRight, FiClock, FiCpu, FiMessageSquare, FiSend, FiShield, FiZap } from 'react-icons/fi';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { agentService } from '../services/agentService';
import { toApiClientError } from '../services/api';
import { providerService } from '../services/providerService';
import { AgentMessage, AgentProfile, AgentThread, ModelDto, ProviderDto, ProviderStatusDto } from '../types';

interface AgentRouteState {
  draftPrompt?: string;
  nonce?: number;
}

export default function Agents() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { refreshSummary, session } = useShell();
  const routeState = (location.state as AgentRouteState | null) ?? null;
  const handledNonceRef = useRef<number | null>(null);
  const [profiles, setProfiles] = useState<AgentProfile[]>([]);
  const [threads, setThreads] = useState<AgentThread[]>([]);
  const [providers, setProviders] = useState<ProviderDto[]>([]);
  const [models, setModels] = useState<ModelDto[]>([]);
  const [providerStatuses, setProviderStatuses] = useState<ProviderStatusDto[]>([]);
  const [selectedAgentId, setSelectedAgentId] = useState('');
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(searchParams.get('thread'));
  const [messages, setMessages] = useState<AgentMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [conversationLoading, setConversationLoading] = useState(false);
  const [runtimeSaving, setRuntimeSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [runtimeProviderCode, setRuntimeProviderCode] = useState('');
  const [runtimeModelCode, setRuntimeModelCode] = useState('');
  const [runtimeVersionLabel, setRuntimeVersionLabel] = useState('');

  const canManageRuntime = session?.role.permissions.includes('agents.runtime.manage') ?? false;

  const selectedAgent = useMemo(() => {
    return profiles.find((agent) => agent.id === selectedAgentId) ?? profiles[0] ?? null;
  }, [profiles, selectedAgentId]);

  const selectedThread = useMemo(() => {
    if (!selectedThreadId) {
      return null;
    }
    return threads.find((thread) => thread.id === selectedThreadId) ?? null;
  }, [selectedThreadId, threads]);

  const runtimeProviders = useMemo(() => {
    return providers.filter((provider) => provider.category === 'text-runtime' && provider.executionSupported);
  }, [providers]);

  const runtimeModels = useMemo(() => {
    return models.filter((model) => model.providerCode === runtimeProviderCode && model.enabledForAgents);
  }, [models, runtimeProviderCode]);

  const selectedProviderStatus = useMemo(() => {
    if (!selectedAgent) {
      return null;
    }
    return providerStatuses.find((item) => item.providerCode === selectedAgent.providerCode) ?? null;
  }, [providerStatuses, selectedAgent]);

  const selectedProvider = useMemo(() => {
    if (!selectedAgent) {
      return null;
    }
    return providers.find((item) => item.code === selectedAgent.providerCode) ?? null;
  }, [providers, selectedAgent]);

  const loadShellData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const catalogRequests: [
        Promise<ProviderDto[]>,
        Promise<ModelDto[]>,
        Promise<ProviderStatusDto[]>,
      ] = canManageRuntime
        ? [
            providerService.findProviders(),
            providerService.findModels(),
            providerService.findProviderStatuses(),
          ]
        : [
            Promise.resolve([]),
            Promise.resolve([]),
            Promise.resolve([]),
          ];

      const [nextProfiles, nextThreads, nextProviders, nextModels, nextProviderStatuses] = await Promise.all([
        agentService.findProfiles(),
        agentService.findThreads(),
        ...catalogRequests,
      ] as const);

      setProfiles(nextProfiles);
      setThreads(nextThreads);
      setProviders(nextProviders);
      setModels(nextModels);
      setProviderStatuses(nextProviderStatuses);

      const routeThreadId = searchParams.get('thread');
      const effectiveThreadId = routeThreadId || nextThreads[0]?.id || null;
      const effectiveAgentId =
        nextThreads.find((thread) => thread.id === effectiveThreadId)?.agentProfileId ||
        nextProfiles[0]?.id ||
        '';

      setSelectedAgentId(effectiveAgentId);
      setSelectedThreadId(effectiveThreadId);

      if (effectiveThreadId) {
        const nextMessages = await agentService.findMessages(effectiveThreadId);
        setMessages(nextMessages);
      } else {
        setMessages([]);
      }
    } catch (loadError) {
      setError(toApiClientError(loadError).message);
      setProfiles([]);
      setThreads([]);
      setMessages([]);
    } finally {
      setLoading(false);
    }
  }, [canManageRuntime, searchParams]);

  useEffect(() => {
    void loadShellData();
  }, [loadShellData]);

  useEffect(() => {
    const routeThreadId = searchParams.get('thread');

    if (!routeThreadId || routeThreadId === selectedThreadId) {
      return;
    }

    void (async () => {
      try {
        setConversationLoading(true);
        const nextMessages = await agentService.findMessages(routeThreadId);
        const nextThread = threads.find((thread) => thread.id === routeThreadId) ?? null;
        setSelectedThreadId(routeThreadId);
        setMessages(nextMessages);
        if (nextThread) {
          setSelectedAgentId(nextThread.agentProfileId);
        }
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
      } finally {
        setConversationLoading(false);
      }
    })();
  }, [searchParams, selectedThreadId, threads]);

  useEffect(() => {
    if (!routeState?.draftPrompt || !routeState.nonce || routeState.nonce === handledNonceRef.current || !selectedAgentId) {
      return;
    }

    handledNonceRef.current = routeState.nonce;
    void (async () => {
      try {
        setConversationLoading(true);
        const conversation = await agentService.createThread({
          agentProfileId: selectedAgentId,
          message: routeState.draftPrompt!,
        });
        const nextThreads = await agentService.findThreads();
        setThreads(nextThreads);
        setSelectedThreadId(conversation.thread.id);
        setMessages(conversation.messages);
        setSearchParams({ thread: conversation.thread.id });
        await refreshSummary();
      } catch (createError) {
        setError(toApiClientError(createError).message);
      } finally {
        setConversationLoading(false);
      }
    })();
  }, [refreshSummary, routeState, selectedAgentId, setSearchParams]);

  useEffect(() => {
    if (!selectedAgent) {
      setRuntimeProviderCode('');
      setRuntimeModelCode('');
      setRuntimeVersionLabel('');
      return;
    }

    setRuntimeProviderCode(selectedAgent.providerCode);
    setRuntimeModelCode(selectedAgent.modelCode);
    setRuntimeVersionLabel(selectedAgent.versionLabel);
  }, [selectedAgent]);

  useEffect(() => {
    if (!runtimeProviderCode) {
      return;
    }

    if (runtimeModels.some((model) => model.code === runtimeModelCode)) {
      return;
    }

    setRuntimeModelCode(runtimeModels[0]?.code ?? '');
  }, [runtimeModelCode, runtimeModels, runtimeProviderCode]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const nextPrompt = draft.trim();

    if (!nextPrompt || !selectedAgentId) {
      return;
    }

    try {
      setConversationLoading(true);
      setError(null);

      const conversation = selectedThreadId
        ? await agentService.appendMessage(selectedThreadId, { message: nextPrompt })
        : await agentService.createThread({ agentProfileId: selectedAgentId, message: nextPrompt });

      setDraft('');
      setSelectedThreadId(conversation.thread.id);
      setMessages(conversation.messages);
      setSearchParams({ thread: conversation.thread.id });

      const nextThreads = await agentService.findThreads();
      setThreads(nextThreads);
      await refreshSummary();
    } catch (submitError) {
      setError(toApiClientError(submitError).message);
    } finally {
      setConversationLoading(false);
    }
  };

  const handleSelectAgent = (agentId: string) => {
    setSelectedAgentId(agentId);

    const threadForAgent = threads.find((thread) => thread.agentProfileId === agentId);

    if (threadForAgent) {
      setSelectedThreadId(threadForAgent.id);
      setSearchParams({ thread: threadForAgent.id });
      void (async () => {
        try {
          setConversationLoading(true);
          const nextMessages = await agentService.findMessages(threadForAgent.id);
          setMessages(nextMessages);
        } catch (loadError) {
          setError(toApiClientError(loadError).message);
        } finally {
          setConversationLoading(false);
        }
      })();
      return;
    }

    setSelectedThreadId(null);
    setSearchParams({});
    setMessages([]);
  };

  const handleSelectThread = (threadId: string, agentId: string) => {
    setSelectedThreadId(threadId);
    setSelectedAgentId(agentId);
    setSearchParams({ thread: threadId });
    void (async () => {
      try {
        setConversationLoading(true);
        const nextMessages = await agentService.findMessages(threadId);
        setMessages(nextMessages);
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
      } finally {
        setConversationLoading(false);
      }
    })();
  };

  const handleRuntimeSave = async () => {
    if (!selectedAgent || !runtimeProviderCode || !runtimeModelCode) {
      return;
    }

    try {
      setRuntimeSaving(true);
      setError(null);

      const updatedProfile = await agentService.updateRuntime(selectedAgent.id, {
        providerCode: runtimeProviderCode,
        modelCode: runtimeModelCode,
        versionLabel: runtimeVersionLabel,
      });

      setProfiles((current) => current.map((profile) => (
        profile.id === updatedProfile.id ? updatedProfile : profile
      )));

      const nextThreads = await agentService.findThreads();
      setThreads(nextThreads);
      await refreshSummary();
    } catch (runtimeError) {
      setError(toApiClientError(runtimeError).message);
    } finally {
      setRuntimeSaving(false);
    }
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
      <aside className="space-y-4">
        <WorkspaceNotice
          title="Agents com runtime versionado."
          description="Perfis, threads e mensagens persistem no backend. Cada agente agora expõe provider, modelo, estilo de API, readiness, streaming e estado de credencial de forma explicita."
          state={selectedAgent?.availability ?? 'preview'}
          detail="Quando a credencial existe, a thread usa inferencia real. Fallback continua opt-in por chamada, e providers fora do runtime real ficam bloqueados no seletor."
        />

        <div className="shell-surface p-5">
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Agents</p>
            <StatusBadge state={selectedAgent?.availability ?? 'preview'} />
          </div>
          <h2 className="mt-3 text-2xl font-semibold text-[var(--ink-strong)]">Runtime real por perfil</h2>
              <p className="mt-3 text-sm leading-6 text-[var(--ink-soft)]">
                O runtime do agente ja informa provider, modelo, apiStyle, credentialState, catalogState, readiness e suporte a streaming. So os providers realmente suportados e configurados entram como `live`.
              </p>
              <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                Governanca explicita: live, implemented_with_restrictions, catalog_only, blocked e out_of_scope.
              </p>
        </div>

        {profiles.map((agent) => (
          <button
            key={agent.id}
            type="button"
            className={[
              'w-full rounded-[24px] border px-5 py-5 text-left transition-all duration-200',
              agent.id === selectedAgentId ? 'bg-white shadow-panel' : 'bg-white/80 hover:-translate-y-0.5 hover:bg-white',
            ].join(' ')}
            style={{ borderColor: 'var(--line-soft)' }}
            onClick={() => handleSelectAgent(agent.id)}
          >
            <div className="flex items-center justify-between gap-3">
              <p className="text-base font-semibold text-[var(--ink-strong)]">{agent.name}</p>
              <StatusBadge state={agent.availability} />
            </div>
            <p className="mt-2 text-sm font-medium text-[var(--ink-strong)]">{agent.specialty}</p>
            <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">{agent.description}</p>
            <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
              <span>{agent.providerCode}</span>
              <span>{agent.apiStyle}</span>
              <span>{agent.credentialState}</span>
              <span>{agent.catalogState}</span>
              <span>{agent.versionLabel}</span>
            </div>
            <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">{agent.modelCode}</p>
            {agent.toolset.length > 0 ? (
              <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                {agent.toolset.join(' • ')}
              </p>
            ) : null}
            <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">{agent.note}</p>
          </button>
        ))}

        <div className="shell-surface p-5">
          <div className="flex items-center justify-between gap-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Threads</p>
            <span className="text-xs font-semibold text-[var(--ink-soft)]">{threads.length}</span>
          </div>

          <div className="mt-4 space-y-2">
            {threads.length === 0 ? (
              <p className="text-sm leading-6 text-[var(--ink-soft)]">Nenhuma thread criada ainda. Envie um prompt para abrir a primeira conversa persistida.</p>
            ) : (
              threads.map((thread) => (
                <button
                  key={thread.id}
                  type="button"
                  className={[
                    'w-full rounded-[20px] border px-4 py-4 text-left transition-all duration-200',
                    thread.id === selectedThreadId ? 'bg-[var(--surface-muted)]' : 'bg-white hover:bg-[var(--surface-muted)]',
                  ].join(' ')}
                  style={{ borderColor: 'var(--line-soft)' }}
                  onClick={() => handleSelectThread(thread.id, thread.agentProfileId)}
                >
                  <p className="text-sm font-semibold text-[var(--ink-strong)]">{thread.title}</p>
                  <p className="mt-1 text-sm leading-6 text-[var(--ink-soft)]">{thread.lastMessagePreview}</p>
                  <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                    {thread.providerCode ? <span>{thread.providerCode}</span> : null}
                    {thread.apiStyle ? <span>{thread.apiStyle}</span> : null}
                    {thread.credentialState ? <span>{thread.credentialState}</span> : null}
                    {thread.versionLabel ? <span>{thread.versionLabel}</span> : null}
                  </div>
                  <p className="mt-3 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">{thread.updatedAt}</p>
                </button>
              ))
            )}
          </div>
        </div>

        <div className="shell-panel p-5">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Atalho</p>
          <Link to="/library" className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-[var(--ink-strong)]">
            Abrir biblioteca
            <FiArrowRight size={16} />
          </Link>
        </div>
      </aside>

      <section className="shell-surface flex min-h-[720px] flex-col overflow-hidden">
        <div className="border-b px-5 py-5 sm:px-7" style={{ borderColor: 'var(--line-soft)' }}>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Agent ativo</p>
              <h1 className="mt-2 text-2xl font-semibold text-[var(--ink-strong)]">{selectedAgent?.name ?? 'Agents'}</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--ink-soft)]">{selectedAgent?.description ?? 'Selecione um perfil para iniciar uma thread.'}</p>
              {selectedAgent ? (
              <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                  <span>{selectedAgent.providerCode}</span>
                  <span>{selectedAgent.modelCode}</span>
                  <span>{selectedAgent.apiStyle}</span>
                  <span>{selectedAgent.credentialState}</span>
                  <span>{selectedAgent.catalogState}</span>
                  {selectedProvider ? <span>{selectedProvider.implementationStatus}</span> : null}
                  {selectedProvider ? <span>{selectedProvider.evidenceLevel}</span> : null}
                  <span>{selectedAgent.versionLabel}</span>
                </div>
              ) : null}
            </div>
            <div className="flex flex-wrap gap-3">
              {selectedAgent ? (
                <div className="rounded-full border bg-white px-4 py-2 text-sm font-semibold text-[var(--ink-strong)]" style={{ borderColor: 'var(--line-soft)' }}>
                  {selectedAgent.specialty}
                </div>
              ) : null}
              <StatusBadge state={selectedAgent?.availability ?? 'preview'} />
            </div>
          </div>
        </div>

        {canManageRuntime && selectedAgent ? (
          <div className="border-b px-5 py-5 sm:px-7" style={{ borderColor: 'var(--line-soft)' }}>
            <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_220px_auto]">
              <label className="space-y-2 text-sm">
                <span className="font-semibold text-[var(--ink-strong)]">Provider</span>
                <select
                  aria-label="Selecionar provider do agent"
                  className="shell-input min-h-[48px]"
                  value={runtimeProviderCode}
                  onChange={(event) => setRuntimeProviderCode(event.target.value)}
                  disabled={runtimeSaving}
                >
                  {runtimeProviders.map((provider) => (
                    <option key={provider.code} value={provider.code}>
                      {provider.name}
                    </option>
                  ))}
                </select>
              </label>

              <label className="space-y-2 text-sm">
                <span className="font-semibold text-[var(--ink-strong)]">Modelo</span>
                <select
                  aria-label="Selecionar modelo do agent"
                  className="shell-input min-h-[48px]"
                  value={runtimeModelCode}
                  onChange={(event) => setRuntimeModelCode(event.target.value)}
                  disabled={runtimeSaving}
                >
                  {runtimeModels.map((model) => (
                    <option key={model.code} value={model.code}>
                      {model.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="space-y-2 text-sm">
                <span className="font-semibold text-[var(--ink-strong)]">Versao</span>
                <input
                  aria-label="Versao do agent"
                  className="shell-input min-h-[48px]"
                  value={runtimeVersionLabel}
                  onChange={(event) => setRuntimeVersionLabel(event.target.value)}
                  disabled={runtimeSaving}
                />
              </label>

              <div className="flex items-end">
                <button type="button" className="btn-primary w-full lg:w-auto" onClick={() => void handleRuntimeSave()} disabled={runtimeSaving}>
                  <FiCpu size={16} />
                  {runtimeSaving ? 'Salvando...' : 'Salvar runtime'}
                </button>
              </div>
            </div>

            {selectedProviderStatus ? (
              <div className="mt-4 flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                <FiShield size={12} />
                <span>{selectedProviderStatus.category}</span>
                <span>{selectedProviderStatus.readinessStatus}</span>
                <span>{selectedProviderStatus.configured ? 'configured' : 'missing_credentials'}</span>
                <span>{selectedProviderStatus.executionSupported ? 'execution_supported' : 'manual_only'}</span>
                <span>{selectedProviderStatus.implementationStatus}</span>
                <span>{selectedProviderStatus.evidenceLevel}</span>
                <span>{selectedProviderStatus.streamingMode}</span>
                <span>{selectedProviderStatus.runtimeMaturity}</span>
              </div>
            ) : null}
          </div>
        ) : null}

        <div className="flex-1 space-y-4 overflow-y-auto px-5 py-6 sm:px-7">
          {loading || conversationLoading ? (
            <div className="rounded-[20px] border border-dashed px-4 py-3 text-sm text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
              Carregando conversa real do workspace...
            </div>
          ) : error ? (
            <div className="rounded-[20px] border border-dashed px-4 py-3 text-sm text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
              {error}
            </div>
          ) : messages.length === 0 ? (
            <div className="rounded-[20px] border border-dashed px-4 py-3 text-sm text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
              Nenhuma thread ativa ainda. Use um atalho abaixo ou envie um prompt para criar a primeira conversa persistida.
            </div>
          ) : (
            messages.map((message) => {
              if (message.role === 'system') {
                return (
                  <div key={message.id} className="rounded-[20px] border border-dashed px-4 py-3 text-sm text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
                    {message.body}
                  </div>
                );
              }

              const isUser = message.role === 'user';

              return (
                <div key={message.id} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={[
                      'max-w-3xl rounded-[26px] px-5 py-4',
                      isUser ? 'bg-[var(--action-dark)] text-white' : 'border bg-[var(--surface-muted)] text-[var(--ink-strong)]',
                    ].join(' ')}
                    style={isUser ? undefined : { borderColor: 'var(--line-soft)' }}
                  >
                    <p className="text-sm leading-7">{message.body}</p>
                    <div className={`mt-3 flex items-center gap-2 text-xs ${isUser ? 'text-white/70' : 'text-[var(--ink-soft)]'}`}>
                      <FiClock size={12} />
                      {message.timestamp}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="border-t px-5 py-5 sm:px-7" style={{ borderColor: 'var(--line-soft)' }}>
          <div className="mb-4 flex flex-wrap gap-3">
            <button type="button" className="pill-button" onClick={() => setDraft('Mapeie um fluxo de onboarding com checkpoints de aprovacao.')}>
              <FiZap size={16} />
              Onboarding
            </button>
            <button type="button" className="pill-button" onClick={() => setDraft('Compare risco, custo e velocidade para publicar este fluxo.')}>
              <FiZap size={16} />
              Trade-offs
            </button>
            <button type="button" className="pill-button" onClick={() => setDraft('Resuma tudo o que preciso saber antes de executar esta tarefa.')}>
              <FiZap size={16} />
              Resumo
            </button>
          </div>

          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={(event) => void handleSubmit(event)}>
            <textarea
              aria-label="Enviar prompt"
              autoComplete="off"
              className="shell-input min-h-[120px] resize-none sm:min-h-0 sm:flex-1"
              placeholder="Envie uma instrucao para o agent selecionado."
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
            />
            <button type="submit" className="btn-primary sm:self-end" disabled={conversationLoading || !selectedAgent}>
              Enviar
              <FiSend size={16} />
            </button>
          </form>

          {selectedThread ? (
            <div className="mt-4 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
              <FiMessageSquare size={12} />
              Thread ativa: {selectedThread.title}
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
