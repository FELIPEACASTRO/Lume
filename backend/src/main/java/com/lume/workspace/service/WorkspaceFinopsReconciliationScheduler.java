package com.lume.workspace.service;

import com.lume.infrastructure.config.FinopsReconciliationProperties;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkspaceFinopsReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFinopsReconciliationScheduler.class);

    private final FinopsReconciliationProperties properties;
    private final WorkspaceJpaRepository workspaceRepository;
    private final WorkspaceFinopsReconciliationExecutor reconciliationExecutor;

    public WorkspaceFinopsReconciliationScheduler(
            FinopsReconciliationProperties properties,
            WorkspaceJpaRepository workspaceRepository,
            WorkspaceFinopsReconciliationExecutor reconciliationExecutor
    ) {
        this.properties = properties;
        this.workspaceRepository = workspaceRepository;
        this.reconciliationExecutor = reconciliationExecutor;
    }

    @Scheduled(cron = "${lume.finops.reconciliation.cron:0 30 2 1 * *}")
    public void runMonthlyReconciliation() {
        if (!properties.isEnabled()) {
            return;
        }

        List<WorkspaceJpaEntity> workspaces = workspaceRepository.findAll();
        int processed = 0;
        int maxToProcess = Math.max(1, properties.getMaxWorkspacesPerRun());

        for (WorkspaceJpaEntity workspace : workspaces) {
            if (processed >= maxToProcess) {
                break;
            }

            try {
                var response = reconciliationExecutor.runForWorkspaceSystem(
                        workspace.getId(),
                        properties.isAutoFixCreditDrift()
                );
                log.info(
                        "FinOps reconciliacao scheduler workspaceId={} status={} creditDrift={} autoFix={}",
                        workspace.getId(),
                        response.reconciliationStatus(),
                        response.creditDrift(),
                        properties.isAutoFixCreditDrift()
                );
            } catch (Exception ex) {
                log.warn(
                        "Falha na reconciliacao FinOps agendada workspaceId={} motivo={}",
                        workspace.getId(),
                        ex.getMessage()
                );
            }

            processed++;
        }
    }
}
