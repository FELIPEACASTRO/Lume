package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.entity.WorkspaceBudgetJpaEntity;
import com.lume.workspace.repository.WorkspaceBudgetJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BudgetLimitAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspaceBudgetJpaRepository budgetRepository;
    private final WorkspaceMeteringService workspaceMeteringService;

    public BudgetLimitAnomalyDetector(
            WorkspaceBudgetJpaRepository budgetRepository,
            WorkspaceMeteringService workspaceMeteringService
    ) {
        this.budgetRepository = budgetRepository;
        this.workspaceMeteringService = workspaceMeteringService;
    }

    @Override
    public List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now) {
        List<FinopsAnomalyResponse> anomalies = new ArrayList<>();

        WorkspaceBudgetJpaEntity budget = budgetRepository.findByWorkspaceId(workspaceId).orElse(null);
        int consumedCredits = workspaceMeteringService.currentSnapshot().consumedCredits();
        if (budget != null) {
            if (consumedCredits >= budget.getHardLimitCredits()) {
                anomalies.add(FinopsAnomalyDetector.anomaly(
                        "budget_hard_limit_reached",
                        "critical",
                        "Budget hard limit atingido",
                        "Consumo atual acima do limite hard do workspace.",
                        "Aplicar kill switch de capacidade cara e revisar plano/credito adicional.",
                        now
                ));
            } else if (consumedCredits >= budget.getSoftLimitCredits()) {
                anomalies.add(FinopsAnomalyDetector.anomaly(
                        "budget_soft_limit_reached",
                        "warning",
                        "Budget soft limit atingido",
                        "Consumo atual encostou no limite soft do workspace.",
                        "Revisar policy de roteamento e alertar owner do workspace.",
                        now
                ));
            }
        }

        return anomalies;
    }
}
