package com.lume.workspace.service;

import com.lume.infrastructure.config.FinopsReconciliationProperties;
import com.lume.workspace.dto.FinopsReconciliationResponse;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceFinopsReconciliationScheduler - Unit Tests")
class WorkspaceFinopsReconciliationSchedulerTest {

    @Mock
    private WorkspaceJpaRepository workspaceRepository;

    @Mock
    private WorkspaceFinopsReconciliationExecutor reconciliationExecutor;

    @Test
    @DisplayName("deve ignorar reconciliacao quando feature estiver desabilitada")
    void shouldSkipRunWhenDisabled() {
        FinopsReconciliationProperties properties = new FinopsReconciliationProperties();
        properties.setEnabled(false);
        WorkspaceFinopsReconciliationScheduler scheduler = new WorkspaceFinopsReconciliationScheduler(
                properties,
                workspaceRepository,
                reconciliationExecutor
        );

        scheduler.runMonthlyReconciliation();

        verify(workspaceRepository, never()).findAll();
        verify(reconciliationExecutor, never()).runForWorkspaceSystem(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("deve processar workspaces respeitando auto-fix habilitado")
    void shouldProcessWorkspacesWithAutoFix() {
        FinopsReconciliationProperties properties = new FinopsReconciliationProperties();
        properties.setEnabled(true);
        properties.setAutoFixCreditDrift(true);
        properties.setMaxWorkspacesPerRun(10);
        WorkspaceFinopsReconciliationScheduler scheduler = new WorkspaceFinopsReconciliationScheduler(
                properties,
                workspaceRepository,
                reconciliationExecutor
        );

        when(workspaceRepository.findAll()).thenReturn(List.of(workspace(10L), workspace(20L)));
        when(reconciliationExecutor.runForWorkspaceSystem(anyLong(), anyBoolean()))
                .thenReturn(balancedResponse());

        scheduler.runMonthlyReconciliation();

        verify(reconciliationExecutor, times(1)).runForWorkspaceSystem(10L, true);
        verify(reconciliationExecutor, times(1)).runForWorkspaceSystem(20L, true);
    }

    @Test
    @DisplayName("deve limitar quantidade de workspaces processados por execucao")
    void shouldRespectMaxWorkspacesPerRun() {
        FinopsReconciliationProperties properties = new FinopsReconciliationProperties();
        properties.setEnabled(true);
        properties.setAutoFixCreditDrift(false);
        properties.setMaxWorkspacesPerRun(1);
        WorkspaceFinopsReconciliationScheduler scheduler = new WorkspaceFinopsReconciliationScheduler(
                properties,
                workspaceRepository,
                reconciliationExecutor
        );

        when(workspaceRepository.findAll()).thenReturn(List.of(workspace(10L), workspace(20L)));
        when(reconciliationExecutor.runForWorkspaceSystem(anyLong(), anyBoolean()))
                .thenReturn(balancedResponse());

        scheduler.runMonthlyReconciliation();

        verify(reconciliationExecutor, times(1)).runForWorkspaceSystem(10L, false);
        verify(reconciliationExecutor, never()).runForWorkspaceSystem(20L, false);
    }

    private WorkspaceJpaEntity workspace(Long id) {
        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity();
        workspace.setId(id);
        workspace.setName("Workspace " + id);
        workspace.setSlug("workspace-" + id);
        workspace.setOrganizationId(1L);
        return workspace;
    }

    private FinopsReconciliationResponse balancedResponse() {
        return new FinopsReconciliationResponse(
                "balanced",
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                0,
                0,
                BigDecimal.ZERO,
                0,
                0,
                false,
                null,
                "ok",
                "2026-03-10T00:00:00"
        );
    }
}
