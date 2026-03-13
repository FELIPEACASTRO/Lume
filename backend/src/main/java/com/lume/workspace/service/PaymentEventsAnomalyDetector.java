package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentEventsAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspacePaymentEventJpaRepository paymentEventRepository;

    public PaymentEventsAnomalyDetector(WorkspacePaymentEventJpaRepository paymentEventRepository) {
        this.paymentEventRepository = paymentEventRepository;
    }

    @Override
    public List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now) {
        List<FinopsAnomalyResponse> anomalies = new ArrayList<>();

        long pendingPaymentEvents = paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(workspaceId, "pending");
        if (pendingPaymentEvents > 0) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "payment_events_pending",
                    "warning",
                    "Eventos de pagamento pendentes",
                    "Existem webhooks de pagamento pendentes de processamento.",
                    "Reprocessar fila de webhook e validar retries do gateway.",
                    now
            ));
        }

        long orphanPaymentEvents = paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(workspaceId);
        if (orphanPaymentEvents > 0) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "payment_events_orphan",
                    "warning",
                    "Eventos de pagamento orfaos",
                    "Existem eventos financeiros sem vinculo de invoice.",
                    "Conciliar eventos com invoices para evitar drift comercial.",
                    now
            ));
        }

        return anomalies;
    }
}
