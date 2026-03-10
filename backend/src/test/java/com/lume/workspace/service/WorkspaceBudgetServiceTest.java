package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.dto.BudgetSummaryResponse;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import com.lume.workspace.entity.WorkspaceBudgetJpaEntity;
import com.lume.workspace.repository.WorkspaceBudgetJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WorkspaceBudgetService - Unit Tests")
class WorkspaceBudgetServiceTest {

    @Test
    @DisplayName("should summarize healthy budget for workspace consumption")
    void shouldSummarizeHealthyBudgetForWorkspaceConsumption() {
        WorkspaceBudgetJpaRepository repository = mock(WorkspaceBudgetJpaRepository.class);
        WorkspaceBudgetJpaEntity entity = new WorkspaceBudgetJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setCostCenter("core_now");
        entity.setChargebackMode("showback");
        entity.setSoftLimitCredits(320);
        entity.setHardLimitCredits(480);
        when(repository.findByWorkspaceId(1L)).thenReturn(Optional.of(entity));

        WorkspaceBudgetService service = new WorkspaceBudgetService(
                repository,
                new StubWorkspaceMeteringService(180),
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        BudgetSummaryResponse response = service.summarizeForConsumedCredits(180);

        assertThat(response.costCenter()).isEqualTo("core_now");
        assertThat(response.chargebackMode()).isEqualTo("showback");
        assertThat(response.budgetStatus()).isEqualTo("healthy");
        assertThat(response.softLimitUtilizationPercent()).isEqualTo(56);
        assertThat(response.remainingHardCredits()).isEqualTo(300);
    }

    @Test
    @DisplayName("should persist workspace budget updates")
    void shouldPersistWorkspaceBudgetUpdates() {
        WorkspaceBudgetJpaRepository repository = mock(WorkspaceBudgetJpaRepository.class);
        WorkspaceBudgetJpaEntity entity = new WorkspaceBudgetJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setCostCenter("core_now");
        entity.setChargebackMode("showback");
        entity.setSoftLimitCredits(300);
        entity.setHardLimitCredits(450);

        when(repository.findByWorkspaceId(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(WorkspaceBudgetJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceBudgetService service = new WorkspaceBudgetService(
                repository,
                new StubWorkspaceMeteringService(210),
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        BudgetSummaryResponse response = service.updateCurrentBudget(new UpdateWorkspaceBudgetRequest(
                "finops-emea",
                "chargeback",
                280,
                420
        ));

        verify(repository).save(any(WorkspaceBudgetJpaEntity.class));
        assertThat(response.costCenter()).isEqualTo("finops-emea");
        assertThat(response.chargebackMode()).isEqualTo("chargeback");
        assertThat(response.budgetStatus()).isEqualTo("healthy");
    }

    @Test
    @DisplayName("should reject hard limit lower than soft limit")
    void shouldRejectHardLimitLowerThanSoftLimit() {
        WorkspaceBudgetJpaRepository repository = mock(WorkspaceBudgetJpaRepository.class);
        WorkspaceBudgetJpaEntity entity = new WorkspaceBudgetJpaEntity();
        entity.setWorkspaceId(1L);
        entity.setSoftLimitCredits(300);
        entity.setHardLimitCredits(450);
        when(repository.findByWorkspaceId(1L)).thenReturn(Optional.of(entity));

        WorkspaceBudgetService service = new WorkspaceBudgetService(
                repository,
                new StubWorkspaceMeteringService(200),
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        assertThatThrownBy(() -> service.updateCurrentBudget(new UpdateWorkspaceBudgetRequest(
                "core_now",
                "showback",
                400,
                350
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hard limit");
    }

    private static final class StubWorkspaceMeteringService extends WorkspaceMeteringService {
        private final UsageMeteringSnapshot snapshot;

        private StubWorkspaceMeteringService(int consumedCredits) {
            super(null, null, null, null);
            this.snapshot = new UsageMeteringSnapshot(300, consumedCredits, Math.max(0, 300 - consumedCredits), 4, 1, 2);
        }

        @Override
        public UsageMeteringSnapshot currentSnapshot() {
            return snapshot;
        }
    }

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public HttpServletRequest getObject() {
                    return null;
                }
            });
        }

        @Override
        public void requirePermission(String permission) {
        }

        @Override
        public Long getWorkspaceId() {
            return 1L;
        }

        @Override
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }
    }

    private static final class NoOpAuditLogService extends AuditLogService {
        private NoOpAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }
    }
}
