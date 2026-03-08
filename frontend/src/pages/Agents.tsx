import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { FiArrowRight, FiClock, FiMessageSquare, FiSend, FiZap } from 'react-icons/fi';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { agentService } from '../services/agentService';
import { toApiClientError } from '../services/api';
import { AgentMessage, AgentProfile, AgentThread } from '../types';

interface AgentRouteState {
  draftPrompt?: string;
  nonce?: number;
}

export default function Agents() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { refreshSummary } = useShell();
  const routeState = (location.state as AgentRouteState | null) ?? null;
  const handledNonceRef = useRef<number | null>(null);
  const [profiles, setProfiles] = useState<AgentProfile[]>([]);
  const [threads, setThreads] = useState<AgentThread[]>([]);
  const [selectedAgentId, setSelectedAgentId] = useState('');
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(searchParams.get('thread'));
  const [messages, setMessages] = useState<AgentMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [conversationLoading, setConversationLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedAgent = useMemo(() => {
    return profiles.find((agent) => agent.id === selectedAgentId) ?? profiles[0] ?? null;
  }, [profiles, selectedAgentId]);

  const selectedThread = useMemo(() => {
    if (!selectedThreadId) {
      return null;
    }
    return threads.find((thread) => thread.id === selectedThreadId) ?? null;
  }, [selectedThreadId, threads]);

  const loadShellData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [nextProfiles, nextThreads] = await Promise.all([
        agentService.findProfiles(),
        agentService.findThreads(),
      ]);

      setProfiles(nextProfiles);
      setThreads(nextThreads);

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
  }, [searchParams]);

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

  return (
    <div className="grid gap-6 xl:grid-cols-[320px_minmax(0,1fr)]">
      <aside className="space-y-4">
        <WorkspaceNotice
          title="Agents opera em preview assistido."
          description="Perfis, threads e mensagens ja persistem no backend do Lume. A inferencia continua simulada nesta fase para preservar a shell enquanto a camada de providers nao entra."
          state="preview"
          detail="Cada envio cria ou atualiza uma thread real, auditavel e indexavel na busca global."
        />

        <div className="shell-surface p-5">
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Agents</p>
            <StatusBadge state="preview" />
          </div>
          <h2 className="mt-3 text-2xl font-semibold text-[var(--ink-strong)]">Preview assistido</h2>
          <p className="mt-3 text-sm leading-6 text-[var(--ink-soft)]">
            O backend ja sustenta perfis, threads e mensagens. O que segue em preview e a inferencia real com providers externos.
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
              <span>{agent.versionLabel}</span>
            </div>
            <p className="mt-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">{agent.modelCode}</p>
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
              <StatusBadge state="preview" />
            </div>
          </div>
        </div>

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
