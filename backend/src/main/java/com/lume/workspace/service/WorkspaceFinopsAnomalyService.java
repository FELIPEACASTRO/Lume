package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkspaceFinopsAnomalyService {

    private final WorkspaceContextService workspaceContextService;
    private final List<FinopsAnomalyDetector> detectors;

    public WorkspaceFinopsAnomalyService(
            WorkspaceContextService workspaceContextService,
            List<FinopsAnomalyDetector> detectors
    ) {
        this.workspaceContextService = workspaceContextService;
        this.detectors = detectors;
    }

    public List<FinopsAnomalyResponse> listCurrentWorkspace() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        Long workspaceId = workspaceContextService.getWorkspaceId();
        LocalDateTime now = LocalDateTime.now();
        return detectors.stream()
                .flatMap(d -> d.detect(workspaceId, now).stream())
                .toList();
    }
}
