package com.lume.workspace.service;

import com.lume.infrastructure.config.FinopsAnomalyProperties;
import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CostPerRunAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
    private final FinopsAnomalyProperties anomalyProperties;

    public CostPerRunAnomalyDetector(
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
        Double averageCostPerRun = costLedgerRepository.averageEstimatedCostByWorkspaceId(workspaceId);
        if (averageCostPerRun != null && totalRuns >= 5 && averageCostPerRun > anomalyProperties.getCostThreshold()) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "average_cost_per_run_high",
                    "warning",
                    "Custo medio por run elevado",
                    "Custo medio estimado por run acima de US$ " + anomalyProperties.getCostThreshold() + ".",
                    "Priorizar rotas cost-first e revisar modelos premium sem retorno.",
                    now
            ));
        }

        return anomalies;
    }
}
