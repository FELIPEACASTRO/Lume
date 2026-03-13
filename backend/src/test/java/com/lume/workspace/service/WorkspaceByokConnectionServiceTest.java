package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.ByokConnectionResponse;
import com.lume.workspace.dto.CreateByokConnectionRequest;
import com.lume.workspace.dto.UpdateByokConnectionRequest;
import com.lume.workspace.entity.WorkspaceByokConnectionJpaEntity;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.springframework.mock.env.MockEnvironment;
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("WorkspaceByokConnectionService - Unit Tests")
class WorkspaceByokConnectionServiceTest {

    private WorkspaceByokConnectionJpaRepository repository;
    private SecretResolver secretResolver;
    private WorkspaceByokConnectionService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkspaceByokConnectionJpaRepository.class);
        secretResolver = mock(SecretResolver.class);

        when(repository.save(any(WorkspaceByokConnectionJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new WorkspaceByokConnectionService(
                repository,
                new StubWorkspaceContextService(),
                new StubProviderCatalogService(),
                secretResolver,
                new NoOpWorkspaceLedgerService(),
                new NoOpAuditLogService()
        );
    }

    @Test
    @DisplayName("should create with byok- UUID prefix, status=active, health=unknown")
    void shouldCreateWithByokUuidPrefix() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        ByokConnectionResponse response = service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "My OpenAI Key", "OPENAI_API_KEY", null)
        );

        assertThat(response.id()).startsWith("byok-");
        assertThat(response.status()).isEqualTo("active");
        assertThat(response.healthStatus()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("should reject duplicate connection name")
    void shouldRejectDuplicateConnectionName() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "Duplicate Name", "OPENAI_KEY", null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should accept valid secret ref patterns")
    void shouldAcceptValidSecretRefPatterns() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        for (String secretRef : new String[]{"OPENAI_KEY", "_INTERNAL", "A1"}) {
            ByokConnectionResponse response = service.createCurrentWorkspace(
                    new CreateByokConnectionRequest("openai", "Connection " + secretRef, secretRef, null)
            );
            assertThat(response.secretRef()).isEqualTo(secretRef);
        }
    }

    @Test
    @DisplayName("should reject invalid secret ref pattern")
    void shouldRejectInvalidSecretRefPattern() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "Connection", "123-invalid", null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should reject secret ref over 160 chars")
    void shouldRejectSecretRefOver160Chars() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        String longRef = "A" + "B".repeat(160);
        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "Connection", longRef, null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should default scope to workspace when null")
    void shouldDefaultScopeToWorkspace() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        ByokConnectionResponse response = service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "My Key", "OPENAI_KEY", null)
        );

        assertThat(response.scopeLabel()).isEqualTo("workspace");
    }

    @Test
    @DisplayName("should reject invalid scope")
    void shouldRejectInvalidScope() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "My Key", "OPENAI_KEY", "global")
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should reset health on secret ref update")
    void shouldResetHealthOnSecretRefUpdate() {
        WorkspaceByokConnectionJpaEntity existing = new WorkspaceByokConnectionJpaEntity();
        existing.setId("byok-existing");
        existing.setWorkspaceId(1L);
        existing.setProviderCode("openai");
        existing.setConnectionName("My Key");
        existing.setSecretRef("OLD_KEY");
        existing.setScopeLabel("workspace");
        existing.setStatus("active");
        existing.setHealthStatus("healthy");
        existing.setLastError("some old error");

        when(repository.findByIdAndWorkspaceId("byok-existing", 1L)).thenReturn(Optional.of(existing));

        ByokConnectionResponse response = service.updateCurrentWorkspace(
                "byok-existing",
                new UpdateByokConnectionRequest(null, "NEW_SECRET_KEY", null, null)
        );

        assertThat(response.healthStatus()).isEqualTo("unknown");
        assertThat(response.lastError()).isNull();
    }

    @Test
    @DisplayName("should validate as healthy when secret exists")
    void shouldValidateAsHealthy() {
        WorkspaceByokConnectionJpaEntity existing = new WorkspaceByokConnectionJpaEntity();
        existing.setId("byok-val");
        existing.setWorkspaceId(1L);
        existing.setProviderCode("openai");
        existing.setConnectionName("My Key");
        existing.setSecretRef("OPENAI_KEY");
        existing.setScopeLabel("workspace");
        existing.setStatus("active");
        existing.setHealthStatus("unknown");

        when(repository.findByIdAndWorkspaceId("byok-val", 1L)).thenReturn(Optional.of(existing));
        when(secretResolver.resolveOptional("OPENAI_KEY")).thenReturn("sk-secret-value");

        ByokConnectionResponse response = service.validateCurrentWorkspace("byok-val");

        assertThat(response.healthStatus()).isEqualTo("healthy");
        assertThat(response.lastError()).isNull();
    }

    @Test
    @DisplayName("should validate as unhealthy when secret is null")
    void shouldValidateAsUnhealthy() {
        WorkspaceByokConnectionJpaEntity existing = new WorkspaceByokConnectionJpaEntity();
        existing.setId("byok-val");
        existing.setWorkspaceId(1L);
        existing.setProviderCode("openai");
        existing.setConnectionName("My Key");
        existing.setSecretRef("MISSING_KEY");
        existing.setScopeLabel("workspace");
        existing.setStatus("active");
        existing.setHealthStatus("unknown");

        when(repository.findByIdAndWorkspaceId("byok-val", 1L)).thenReturn(Optional.of(existing));
        when(secretResolver.resolveOptional("MISSING_KEY")).thenReturn(null);

        ByokConnectionResponse response = service.validateCurrentWorkspace("byok-val");

        assertThat(response.healthStatus()).isEqualTo("unhealthy");
        assertThat(response.lastError()).isNotNull();
    }

    @Test
    @DisplayName("should reject connection name over 160 chars")
    void shouldRejectConnectionNameOver160Chars() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        String longName = "A".repeat(161);
        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", longName, "OPENAI_KEY", null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should reject blank connection name")
    void shouldRejectBlankConnectionName() {
        when(repository.existsByWorkspaceIdAndConnectionName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.createCurrentWorkspace(
                new CreateByokConnectionRequest("openai", "", "OPENAI_KEY", null)
        )).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw not found for missing connection")
    void shouldThrowNotFoundForMissingConnection() {
        when(repository.findByIdAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentWorkspaceById("byok-nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- Test doubles ----

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
        public String getActorName() {
            return "Lume Operator";
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

    private static final class StubProviderCatalogService extends ProviderCatalogService {
        private StubProviderCatalogService() {
            super(new EnvironmentSecretResolver(new MockEnvironment()));
        }

        @Override
        public String normalizeProviderCode(String providerCode) {
            return providerCode == null ? null : providerCode.trim().toLowerCase();
        }

        @Override
        public ProviderDefinition requireProvider(String providerCode) {
            return new ProviderDefinition(
                    providerCode, providerCode, "llm", null, true, null,
                    java.util.List.of(), null, null, java.util.List.of(),
                    false, false, true, false, "core_live", null, null, null,
                    java.util.List.of(), null
            );
        }

        @Override
        public Optional<ProviderDefinition> findProvider(String providerCode) {
            return Optional.of(requireProvider(providerCode));
        }
    }
}
