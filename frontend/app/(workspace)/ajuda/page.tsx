"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { createSupportTicket, getSettingsUiOptions, listSupportTickets, updateSupportTicket } from "@/lib/api/workspace";

export default function HelpPage() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [severity, setSeverity] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  const ticketsQuery = useQuery({
    queryKey: ["support-tickets"],
    queryFn: () => listSupportTickets(20)
  });

  const uiOptionsQuery = useQuery({
    queryKey: ["settings-ui-options"],
    queryFn: getSettingsUiOptions
  });

  const categoryOptions = useMemo(
    () => uiOptionsQuery.data?.supportCategories ?? [],
    [uiOptionsQuery.data]
  );
  const severityOptions = useMemo(
    () => uiOptionsQuery.data?.supportSeverities ?? [],
    [uiOptionsQuery.data]
  );
  const resolvedStatusCode = useMemo(() => {
    return uiOptionsQuery.data?.supportStatuses.find((status) => status.code === "resolved")?.code ?? "resolved";
  }, [uiOptionsQuery.data]);

  useEffect(() => {
    if (!category && categoryOptions.length > 0) {
      const defaultOption = categoryOptions.find((item) => item.defaultOption) ?? categoryOptions[0];
      setCategory(defaultOption.code);
    }
  }, [category, categoryOptions]);

  useEffect(() => {
    if (!severity && severityOptions.length > 0) {
      const defaultOption = severityOptions.find((item) => item.defaultOption) ?? severityOptions[0];
      setSeverity(defaultOption.code);
    }
  }, [severity, severityOptions]);

  const createTicketMutation = useMutation({
    mutationFn: createSupportTicket,
    onSuccess: () => {
      setFeedback("Ticket aberto com sucesso.");
      setTitle("");
      setDescription("");
      ticketsQuery.refetch();
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao abrir ticket.");
    }
  });

  const resolveTicketMutation = useMutation({
    mutationFn: (ticketId: string) =>
      updateSupportTicket(ticketId, {
        status: resolvedStatusCode,
        resolutionNote: "Resolvido no fluxo operacional do workspace."
      }),
    onSuccess: () => ticketsQuery.refetch()
  });

  function submitTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim() || !description.trim() || !category || !severity) {
      setFeedback("Preencha titulo e descricao para abrir o ticket.");
      return;
    }
    createTicketMutation.mutate({
      title: title.trim(),
      description: description.trim(),
      category,
      severity
    });
  }

  return (
    <>
      <PageHeader
        title="Ajuda e suporte"
        description="Operacao assistida com tickets, diagnostico e proximos passos."
      />
      <div className="grid-2">
        <Panel title="Acoes rapidas" subtitle="Checklist para manter o workspace funcional.">
          <div className="item-list">
            <div className="item-row">
              <strong>1. Validar sessao</strong>
              <p>Use /api/v1/auth/session para confirmar acesso e contexto do workspace.</p>
            </div>
            <div className="item-row">
              <strong>2. Validar providers</strong>
              <p>Abra Admin e rode conectividade nos providers criticos.</p>
            </div>
            <div className="item-row">
              <strong>3. Verificar budget e uso</strong>
              <p>Consulte a pagina Uso para prevenir bloqueios por limite.</p>
            </div>
          </div>
        </Panel>

        <Panel title="Abrir ticket" subtitle="Registre incidentes operacionais com contexto claro.">
          <form className="form-grid" onSubmit={submitTicket}>
            <label>
              Titulo
              <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Ex.: Falha no provider OpenAI" />
            </label>
            <label>
              Categoria
              <select value={category} onChange={(event) => setCategory(event.target.value)}>
                <option value="">Selecione</option>
                {categoryOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Severidade
              <select value={severity} onChange={(event) => setSeverity(event.target.value)}>
                <option value="">Selecione</option>
                {severityOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label style={{ gridColumn: "1 / -1" }}>
              Descricao
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Descreva impacto, endpoint e horario aproximado."
                rows={4}
              />
            </label>
            <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
              <button
                className="button-primary"
                type="submit"
                disabled={
                  createTicketMutation.isPending ||
                  categoryOptions.length === 0 ||
                  severityOptions.length === 0 ||
                  !category ||
                  !severity
                }
              >
                {createTicketMutation.isPending ? "Abrindo ticket..." : "Abrir ticket"}
              </button>
            </div>
          </form>
          {feedback ? <p className="message-info">{feedback}</p> : null}
        </Panel>
      </div>

      <Panel title="Tickets recentes" subtitle="Acompanhe status e fechamento operacional.">
        {ticketsQuery.isLoading ? (
          <p className="muted">Carregando tickets...</p>
        ) : ticketsQuery.data && ticketsQuery.data.length > 0 ? (
          <div className="item-list">
            {ticketsQuery.data.map((ticket) => (
              <div className="item-row" key={ticket.id}>
                <strong>{ticket.title}</strong>
                <p>
                  {ticket.category} · {ticket.severity} · {ticket.status}
                </p>
                <p className="muted">
                  SLA {ticket.slaTargetAt ?? "n/d"} · {ticket.slaBreached ? "fora do SLA" : "dentro do SLA"}
                </p>
                <p className="muted">{ticket.description}</p>
                {ticket.status === "open" || ticket.status === "in_progress" ? (
                  <div style={{ marginTop: "0.45rem" }}>
                    <button
                      className="button-secondary"
                      type="button"
                      onClick={() => resolveTicketMutation.mutate(ticket.id)}
                      disabled={resolveTicketMutation.isPending}
                    >
                      Marcar como resolvido
                    </button>
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        ) : (
          <EmptyState title="Sem tickets" description="Nenhum ticket aberto neste workspace." />
        )}
      </Panel>
    </>
  );
}
