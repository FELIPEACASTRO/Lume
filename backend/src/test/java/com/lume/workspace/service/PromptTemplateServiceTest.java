package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.CreatePromptTemplateRequest;
import com.lume.workspace.dto.PromptTemplateResponse;
import com.lume.workspace.dto.UpdatePromptTemplateRequest;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.PromptTemplateJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PromptTemplateService - Unit Tests")
class PromptTemplateServiceTest {

    @Test
    @DisplayName("should list prompt templates scoped to workspace and agent")
    void shouldListPromptTemplatesScopedToWorkspaceAndAgent() {
        PromptTemplateJpaRepository repository = mock(PromptTemplateJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        AgentProfileJpaRepository agentProfileRepository = mock(AgentProfileJpaRepository.class);

        PromptTemplateJpaEntity template = new PromptTemplateJpaEntity();
        template.setId("tpl-ops");
        template.setWorkspaceId(1L);
        template.setProjectId("proj-ops");
        template.setAgentProfileId("ops");
        template.setTitle("Onboarding");
        template.setSummary("Template operacional.");
        template.setPromptBody("Mapeie {{workspace}}.");
        template.setTemplateScope("workspace");
        template.setStatusLabel("Template operacional");
        template.setAvailability("live");
        template.setOwnerName("Lume Operator");
        template.setVariablesRaw("workspace,owner");
        template.setFavorited(true);
        template.setLastUsedAt(LocalDateTime.now().minusHours(2));

        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId("proj-ops");
        project.setWorkspaceId(1L);
        project.setName("Operacao");

        AgentProfileJpaEntity agent = new AgentProfileJpaEntity();
        agent.setId("ops");
        agent.setWorkspaceId(1L);
        agent.setName("Ops Strategist");

        when(repository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(template));
        when(projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(project));
        when(agentProfileRepository.findByWorkspaceIdOrderByNameAsc(1L)).thenReturn(List.of(agent));

        PromptTemplateService service = new PromptTemplateService(
                repository,
                projectRepository,
                agentProfileRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        List<PromptTemplateResponse> response = service.findAll(null, "proj-ops", "ops", true);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().projectName()).isEqualTo("Operacao");
        assertThat(response.getFirst().agentProfileName()).isEqualTo("Ops Strategist");
        assertThat(response.getFirst().variables()).containsExactly("workspace", "owner");
    }

    @Test
    @DisplayName("should create and mark prompt template usage")
    void shouldCreateAndMarkPromptTemplateUsage() {
        PromptTemplateJpaRepository repository = mock(PromptTemplateJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        AgentProfileJpaRepository agentProfileRepository = mock(AgentProfileJpaRepository.class);

        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId("proj-ops");
        project.setWorkspaceId(1L);
        project.setName("Operacao");

        AgentProfileJpaEntity agent = new AgentProfileJpaEntity();
        agent.setId("ops");
        agent.setWorkspaceId(1L);
        agent.setName("Ops Strategist");

        when(projectRepository.findByIdAndWorkspaceId("proj-ops", 1L)).thenReturn(Optional.of(project));
        when(agentProfileRepository.findByIdAndWorkspaceId("ops", 1L)).thenReturn(Optional.of(agent));
        when(repository.save(any(PromptTemplateJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromptTemplateService service = new PromptTemplateService(
                repository,
                projectRepository,
                agentProfileRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        PromptTemplateResponse created = service.create(new CreatePromptTemplateRequest(
                "Onboarding",
                "Template operacional.",
                "Mapeie {{workspace}}.",
                "workspace",
                "proj-ops",
                "ops",
                List.of("workspace"),
                true,
                null,
                null
        ));

        PromptTemplateJpaEntity persisted = new PromptTemplateJpaEntity();
        persisted.setId(created.id());
        persisted.setWorkspaceId(1L);
        persisted.setProjectId("proj-ops");
        persisted.setAgentProfileId("ops");
        persisted.setTitle("Onboarding");
        persisted.setSummary("Template operacional.");
        persisted.setPromptBody("Mapeie {{workspace}}.");
        persisted.setTemplateScope("workspace");
        persisted.setStatusLabel("Template operacional");
        persisted.setAvailability("live");
        persisted.setOwnerName("Lume Operator");
        persisted.setVariablesRaw("workspace");
        persisted.setFavorited(true);
        when(repository.findByIdAndWorkspaceId(created.id(), 1L)).thenReturn(Optional.of(persisted));

        PromptTemplateResponse touched = service.markUsed(created.id());

        verify(repository, org.mockito.Mockito.atLeast(2)).save(any(PromptTemplateJpaEntity.class));
        assertThat(created.projectId()).isEqualTo("proj-ops");
        assertThat(touched.lastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("should update template favorited state and variables")
    void shouldUpdateTemplateFavoritedStateAndVariables() {
        PromptTemplateJpaRepository repository = mock(PromptTemplateJpaRepository.class);
        ProjectJpaRepository projectRepository = mock(ProjectJpaRepository.class);
        AgentProfileJpaRepository agentProfileRepository = mock(AgentProfileJpaRepository.class);

        PromptTemplateJpaEntity template = new PromptTemplateJpaEntity();
        template.setId("tpl-ops");
        template.setWorkspaceId(1L);
        template.setTitle("Onboarding");
        template.setSummary("Template operacional.");
        template.setPromptBody("Mapeie {{workspace}}.");
        template.setTemplateScope("workspace");
        template.setStatusLabel("Template operacional");
        template.setAvailability("live");
        template.setOwnerName("Lume Operator");
        template.setVariablesRaw("workspace");

        when(repository.findByIdAndWorkspaceId("tpl-ops", 1L)).thenReturn(Optional.of(template));
        when(repository.save(any(PromptTemplateJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromptTemplateService service = new PromptTemplateService(
                repository,
                projectRepository,
                agentProfileRepository,
                new StubWorkspaceContextService(),
                new NoOpAuditLogService()
        );

        PromptTemplateResponse response = service.update("tpl-ops", new UpdatePromptTemplateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("workspace", "owner"),
                true,
                null,
                null
        ));

        assertThat(response.favorited()).isTrue();
        assertThat(response.variables()).containsExactly("workspace", "owner");
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
}
