package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsReconciliationResponse;

public interface WorkspaceFinopsReconciliationExecutor {

    FinopsReconciliationResponse runForWorkspaceSystem(Long workspaceId, boolean applyCreditFix);
}
