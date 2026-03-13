package com.lume.workspace.service;

import com.lume.infrastructure.config.FinopsAnomalyProperties;
import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class InferenceSuccessRateAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
    private final FinopsAnomalyProperties anomalyProperties;

    public InferenceSuccessRateAnomalyDetector(
            WorkspaceCostLedgerEntryJpaRepository costLedgerRepository,
            FinopsAnomalyProperties anomalyProperties
    ) {
        this.costLedgerRepository = costLedgerRepository;
        this.anomalyProperties = anomalyProperties;
    }

    @Override
    public List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now) {
        List<FinopsAnomalyResponse> anomalies = new ArrayList<>();

        long totalRuns = costLedgerRepository.countByWorkspaceId(workspaceId);
        long completedRuns = costLedgerRepository.countByWorkspaceIdAndStatus(workspaceId, "completed");
        if (totalRuns >= 5) {
            double successRate = completedRuns * 100.0 / totalRuns;
            if (successRate < anomalyProperties.getSuccessRateCritical()) {
                anomalies.add(FinopsAnomalyDetector.anomaly(
                        "inference_success_rate_critical",
                        "critical",
                        "Taxa de sucesso de inferencia critica",
                        "Taxa de sucesso abaixo de " + (int) anomalyProperties.getSuccessRateCritical() + "% nas execucoes registradas.",
                        "Investigar providers degradados e fallback policy.",
                        now
                ));
            } else if (successRate < anomalyProperties.getSuccessRateWarning()) {
                anomalies.add(FinopsAnomalyDetector.anomaly(
                        "inference_success_rate_warning",
                        "warning",
                        "Taxa de sucesso de inferencia em atencao",
                        "Taxa de sucesso abaixo de " + (int) anomalyProperties.getSuccessRateWarning() + "% nas execucoes registradas.",
                        "Revisar erros 4xx/5xx por provider e otimizar rotas.",
                        now
                ));
            }
        }

        return anomalies;
    }
}
