"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import {
  getSettingsUiOptions,
  getSubscription,
  listInvoices,
  listPaymentEvents,
  purchaseCreditPack
} from "@/lib/api/workspace";
import { Invoice, PaymentEvent, PurchaseCreditPackResponse } from "@/lib/api/types";

export default function SubscriptionPage() {
  const queryClient = useQueryClient();
  const [packCode, setPackCode] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [lastPurchase, setLastPurchase] = useState<PurchaseCreditPackResponse | null>(null);

  const subscriptionQuery = useQuery({
    queryKey: ["billing-subscription"],
    queryFn: getSubscription
  });

  const invoicesQuery = useQuery({
    queryKey: ["billing-invoices"],
    queryFn: () => listInvoices() as Promise<Invoice[]>
  });

  const paymentEventsQuery = useQuery({
    queryKey: ["billing-payment-events"],
    queryFn: () => listPaymentEvents() as Promise<PaymentEvent[]>
  });

  const uiOptionsQuery = useQuery({
    queryKey: ["settings-ui-options"],
    queryFn: getSettingsUiOptions
  });

  const creditPackOptions = useMemo(
    () => uiOptionsQuery.data?.billingCreditPacks ?? [],
    [uiOptionsQuery.data]
  );

  useEffect(() => {
    if (packCode || creditPackOptions.length === 0) {
      return;
    }
    const defaultPack = creditPackOptions.find((pack) => pack.defaultOption) ?? creditPackOptions[0];
    setPackCode(defaultPack.packCode);
  }, [packCode, creditPackOptions]);

  const selectedPack = useMemo(() => {
    return creditPackOptions.find((pack) => pack.packCode === packCode) ?? null;
  }, [creditPackOptions, packCode]);

  const purchaseMutation = useMutation({
    mutationFn: purchaseCreditPack,
    onSuccess: (response) => {
      setLastPurchase(response);
      setFeedback("Checkout criado. Conclua o pagamento para liberar os creditos.");
      queryClient.invalidateQueries({ queryKey: ["billing-invoices"] });
      queryClient.invalidateQueries({ queryKey: ["billing-payment-events"] });
      queryClient.invalidateQueries({ queryKey: ["billing-subscription"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao criar checkout.");
    }
  });

  function submitPurchase(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedPack) {
      setFeedback("Informe pack, creditos e valor validos.");
      return;
    }
    purchaseMutation.mutate({
      packCode: selectedPack.packCode,
      credits: selectedPack.credits,
      amountBrl: selectedPack.amountBrl,
      description: selectedPack.description?.trim() || undefined
    });
  }

  return (
    <>
      <PageHeader
        title="Assinatura e cobranca"
        description="Visibilidade comercial para plano, faturas e eventos de pagamento."
      />
      <Panel title="Comprar pack de creditos" subtitle="O pagamento entra como pendente e so credita apos webhook valido.">
        <form className="form-grid" onSubmit={submitPurchase}>
          <label>
            Pack
            <select value={packCode} onChange={(event) => setPackCode(event.target.value)}>
              <option value="">Selecione</option>
              {creditPackOptions.map((pack) => (
                <option key={pack.packCode} value={pack.packCode}>
                  {pack.packCode} · {pack.credits} creditos · R$ {Number(pack.amountBrl).toFixed(2)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Creditos
            <input
              type="number"
              min={1}
              value={selectedPack?.credits ?? 0}
              readOnly
            />
          </label>
          <label>
            Valor (BRL)
            <input
              type="number"
              step="0.01"
              min={0.01}
              value={selectedPack?.amountBrl ?? 0}
              readOnly
            />
          </label>
          <label>
            Descricao
            <input value={selectedPack?.description ?? ""} readOnly />
          </label>
          <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
            <button className="button-primary" type="submit" disabled={purchaseMutation.isPending || !selectedPack}>
              {purchaseMutation.isPending ? "Gerando checkout..." : "Gerar checkout Mercado Pago"}
            </button>
          </div>
        </form>
        {feedback ? <p className="message-info">{feedback}</p> : null}
        {lastPurchase ? (
          <div className="item-list">
            <div className="item-row">
              <strong>Status do pagamento</strong>
              <p>{lastPurchase.paymentStatus || "pending"}</p>
              <p className="muted">Referencia: {lastPurchase.externalReference || "-"}</p>
              {lastPurchase.checkoutUrl ? (
                <p>
                  <a href={lastPurchase.checkoutUrl} target="_blank" rel="noreferrer">
                    Abrir checkout
                  </a>
                </p>
              ) : null}
            </div>
          </div>
        ) : null}
      </Panel>
      <div className="grid-2">
        <Panel title="Assinatura atual" subtitle="Status comercial e creditos do workspace.">
          {subscriptionQuery.isLoading ? (
            <p className="muted">Carregando assinatura...</p>
          ) : subscriptionQuery.data ? (
            <div className="item-list">
              <div className="item-row">
                <strong>Plano</strong>
                <p>{subscriptionQuery.data.planLabel}</p>
              </div>
              <div className="item-row">
                <strong>Status</strong>
                <p>{subscriptionQuery.data.subscriptionStatus}</p>
              </div>
              <div className="item-row">
                <strong>Intervalo</strong>
                <p>{subscriptionQuery.data.billingInterval}</p>
              </div>
              <div className="item-row">
                <strong>Creditos</strong>
                <p>
                  {subscriptionQuery.data.totalCredits} (incluidos: {subscriptionQuery.data.includedCredits} · extras:{" "}
                  {subscriptionQuery.data.extraCredits})
                </p>
              </div>
            </div>
          ) : (
            <EmptyState title="Sem assinatura" description="Nao foi encontrada assinatura para o workspace." />
          )}
        </Panel>

        <Panel title="Faturas" subtitle="Ultimas cobrancas registradas no backend.">
          {invoicesQuery.isLoading ? (
            <p className="muted">Carregando faturas...</p>
          ) : invoicesQuery.data && invoicesQuery.data.length > 0 ? (
            <div className="item-list">
              {invoicesQuery.data.map((invoice) => (
                <div className="item-row" key={invoice.id}>
                  <strong>{invoice.invoiceNumber}</strong>
                  <p>
                    {invoice.status} · R$ {Number(invoice.amountBrl).toFixed(2)}
                  </p>
                  <p className="muted">Criada em: {invoice.createdAt}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Sem faturas" description="Ainda nao existem faturas emitidas para o workspace." />
          )}
        </Panel>
      </div>

      <Panel title="Eventos de pagamento" subtitle="Webhook e processamento financeiro recentes.">
        {paymentEventsQuery.isLoading ? (
          <p className="muted">Carregando eventos...</p>
        ) : paymentEventsQuery.data && paymentEventsQuery.data.length > 0 ? (
          <div className="item-list">
            {paymentEventsQuery.data.map((event) => (
              <div className="item-row" key={event.id}>
                <strong>{event.eventType}</strong>
                <p>
                  {event.status} · R$ {Number(event.amountBrl).toFixed(2)}
                </p>
                <p className="muted">Ocorrido em: {event.occurredAt}</p>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState title="Sem eventos" description="Nenhum evento de pagamento processado ate agora." />
        )}
      </Panel>
    </>
  );
}
