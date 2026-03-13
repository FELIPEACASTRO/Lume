package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubscriptionAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspaceSubscriptionJpaRepository subscriptionRepository;

    public SubscriptionAnomalyDetector(WorkspaceSubscriptionJpaRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now) {
        List<FinopsAnomalyResponse> anomalies = new ArrayList<>();

        if (subscriptionRepository.findByWorkspaceId(workspaceId).isEmpty()) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "missing_subscription",
                    "warning",
                    "Workspace sem assinatura comercial",
                    "Nao existe assinatura ativa para o workspace.",
                    "Inicializar assinatura antes de escalar consumo e billing.",
                    now
            ));
        }

        return anomalies;
    }
}
