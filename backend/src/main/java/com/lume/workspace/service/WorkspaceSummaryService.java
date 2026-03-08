package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.RecentItemResponse;
import com.lume.workspace.dto.SummaryCountsResponse;
import com.lume.workspace.dto.WorkspaceFacetResponse;
import com.lume.workspace.dto.WorkspaceSummaryResponse;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceSummaryService {

    private final JpaUserRepository userRepository;
    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final ProjectJpaRepository projectRepository;
    private final TaskJpaRepository taskRepository;
    private final NotificationJpaRepository notificationRepository;
    private final MembershipJpaRepository membershipRepository;
    private final WorkspaceContextService workspaceContextService;

    public WorkspaceSummaryService(
            JpaUserRepository userRepository,
            LibraryEntryJpaRepository libraryEntryRepository,
            AgentThreadJpaRepository agentThreadRepository,
            ProjectJpaRepository projectRepository,
            TaskJpaRepository taskRepository,
            NotificationJpaRepository notificationRepository,
            MembershipJpaRepository membershipRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.userRepository = userRepository;
        this.libraryEntryRepository = libraryEntryRepository;
        this.agentThreadRepository = agentThreadRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public WorkspaceSummaryResponse getSummary() {
        Long workspaceId = workspaceContextService.getWorkspaceId();
        List<UserJpaEntity> users = userRepository.findByIdInOrderByNameAsc(
                membershipRepository.findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc(workspaceId)
                        .stream()
                        .map(membership -> membership.getUserId())
                        .toList()
        );
        int userCount = users.size();
        int libraryCount = libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).size();
        int threadCount = agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).size();
        int projectCount = projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).size();
        int taskCount = taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).size();
        int unreadNotifications = (int) notificationRepository.countByWorkspaceIdAndReadFalse(workspaceId);
        long activeUsers = users.stream().filter(UserJpaEntity::isActive).count();

        List<RecentItemResponse> recentItems = List.of(
                new RecentItemResponse(
                        "search-live",
                        "Busca global",
                        "Agora cruza rotas, usuarios, biblioteca e threads reais do workspace.",
                        "Ctrl/Cmd + K",
                        "/",
                        "live"
                ),
                new RecentItemResponse(
                        "users-live",
                        "Operacao de usuarios",
                        "%d usuarios cadastrados, %d ativos no workspace atual.".formatted(userCount, activeUsers),
                        "API real em /users",
                        "/users",
                        "live"
                ),
                new RecentItemResponse(
                        "projects-live",
                        "Projetos",
                        "%d projetos reais organizam as frentes do workspace atual.".formatted(projectCount),
                        "Backlog e ownership centralizados",
                        "/projects",
                        "live"
                ),
                new RecentItemResponse(
                        "tasks-preview",
                        "Tarefas",
                        "%d tarefas persistidas com steps visiveis em modo assistido.".formatted(taskCount),
                        "Preview controlado de task execution",
                        "/tasks",
                        "preview"
                ),
                new RecentItemResponse(
                        "library-live",
                        "Biblioteca",
                        "%d documentos persistidos e prontos para busca e filtros.".formatted(libraryCount),
                        "API real em /library/entries",
                        "/library",
                        "live"
                ),
                new RecentItemResponse(
                        "usage-live",
                        "Uso e inbox",
                        "%d notificacoes abertas e trilha operacional pronta para o topo da shell.".formatted(unreadNotifications),
                        "Usage e inbox reais",
                        "/usage",
                        "live"
                )
        );

        List<WorkspaceFacetResponse> facets = List.of(
                new WorkspaceFacetResponse(
                        "facet-workspace",
                        "Workspace",
                        "Hub de trabalho",
                        "Entrada principal para busca, usuarios, biblioteca e agents do workspace atual.",
                        "live",
                        "/"
                ),
                new WorkspaceFacetResponse(
                        "facet-users",
                        "Usuarios",
                        "Modulo real",
                        "CRUD real e escopado ao workspace principal desta instalacao.",
                        "live",
                        "/users"
                ),
                new WorkspaceFacetResponse(
                        "facet-projects",
                        "Projetos",
                        "Organizacao real",
                        "Projetos passaram a estruturar ownership e agrupamento das tarefas do workspace.",
                        "live",
                        "/projects"
                ),
                new WorkspaceFacetResponse(
                        "facet-tasks",
                        "Tarefas",
                        "Execucao assistida",
                        "Composer principal agora cria tarefas persistidas com steps, follow-ups e task view dedicada.",
                        "preview",
                        "/tasks"
                ),
                new WorkspaceFacetResponse(
                        "facet-library",
                        "Biblioteca",
                        "Contexto persistido",
                        "Documentos e tags agora vem do backend, sem depender de fixtures locais.",
                        "live",
                        "/library"
                )
        );

        return new WorkspaceSummaryResponse(
                workspaceContextService.getWorkspaceName(),
                workspaceContextService.getOrganizationName(),
                new SummaryCountsResponse(userCount, libraryCount, threadCount, projectCount, taskCount, unreadNotifications),
                recentItems,
                facets
        );
    }
}
