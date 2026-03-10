package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.RecentItemResponse;
import com.lume.workspace.dto.SummaryCountsResponse;
import com.lume.workspace.dto.WorkspaceFacetResponse;
import com.lume.workspace.dto.WorkspaceSummaryResponse;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.NotificationJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class WorkspaceSummaryService {

    private static final DateTimeFormatter SUMMARY_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

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
        int libraryCount = (int) libraryEntryRepository.countByWorkspaceId(workspaceId);
        int threadCount = (int) agentThreadRepository.countByWorkspaceId(workspaceId);
        int projectCount = (int) projectRepository.countByWorkspaceId(workspaceId);
        int taskCount = (int) taskRepository.countByWorkspaceId(workspaceId);
        int unreadNotifications = (int) notificationRepository.countByWorkspaceIdAndReadFalse(workspaceId);
        long activeUsers = users.stream().filter(UserJpaEntity::isActive).count();

        List<ProjectJpaEntity> projects = projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        List<TaskJpaEntity> tasks = taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        List<LibraryEntryJpaEntity> libraryEntries = libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        List<AgentThreadJpaEntity> threads = agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        List<NotificationJpaEntity> notifications = notificationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        List<RecentCandidate> recentCandidates = new ArrayList<>();
        tasks.stream().limit(3).forEach(task -> recentCandidates.add(new RecentCandidate(
                task.getUpdatedAt(),
                new RecentItemResponse(
                        "task-" + task.getId(),
                        task.getTitle(),
                        task.getSummary(),
                        buildRecentTaskDetail(task),
                        "/tasks/" + task.getId(),
                        task.getAvailability()
                )
        )));
        libraryEntries.stream().limit(2).forEach(entry -> recentCandidates.add(new RecentCandidate(
                entry.getUpdatedAt(),
                new RecentItemResponse(
                        "library-" + entry.getId(),
                        entry.getTitle(),
                        entry.getSummary(),
                        "Biblioteca . " + formatMoment(entry.getUpdatedAt()),
                        "/library?entry=" + entry.getId(),
                        entry.getAvailability()
                )
        )));
        threads.stream().limit(2).forEach(thread -> recentCandidates.add(new RecentCandidate(
                thread.getUpdatedAt(),
                new RecentItemResponse(
                        "thread-" + thread.getId(),
                        thread.getTitle(),
                        thread.getLastMessagePreview() == null || thread.getLastMessagePreview().isBlank()
                                ? "Conversa registrada."
                                : thread.getLastMessagePreview(),
                        "Agentes . " + formatMoment(thread.getUpdatedAt()),
                        "/agents?thread=" + thread.getId(),
                        thread.getAvailability()
                )
        )));
        projects.stream().limit(2).forEach(project -> recentCandidates.add(new RecentCandidate(
                project.getUpdatedAt(),
                new RecentItemResponse(
                        "project-" + project.getId(),
                        project.getName(),
                        project.getSummary(),
                        "Projetos . " + formatMoment(project.getUpdatedAt()),
                        "/projects?project=" + project.getId(),
                        project.getAvailability()
                )
        )));
        notifications.stream().limit(2).forEach(notification -> recentCandidates.add(new RecentCandidate(
                notification.getCreatedAt(),
                new RecentItemResponse(
                        "notification-" + notification.getId(),
                        notification.getTitle(),
                        notification.getBody(),
                        "Inbox . " + formatMoment(notification.getCreatedAt()),
                        notification.getPath(),
                        "live"
                )
        )));

        List<RecentItemResponse> recentItems = recentCandidates.stream()
                .sorted(Comparator.comparing(RecentCandidate::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(RecentCandidate::item)
                .toList();

        List<WorkspaceFacetResponse> facets = new ArrayList<>();
        facets.add(new WorkspaceFacetResponse(
                "facet-members",
                "Equipe",
                activeUsers + " usuarios ativos",
                userCount == 0
                        ? "Nenhuma pessoa foi adicionada a este workspace ainda."
                        : "Veja quem tem acesso e quais funcoes estao em uso.",
                "live",
                "/users"
        ));
        facets.add(new WorkspaceFacetResponse(
                "facet-projects",
                "Projetos",
                projectCount + " projetos persistidos",
                projectCount == 0
                        ? "Nenhum projeto foi registrado ainda neste workspace."
                        : "Abra um projeto para acompanhar contexto, responsaveis e andamento.",
                "live",
                "/projects"
        ));
        facets.add(new WorkspaceFacetResponse(
                "facet-tasks",
                "Tarefas",
                taskCount + " tarefas no workspace",
                taskCount == 0
                        ? "Ainda nao ha tarefas persistidas."
                        : "A lista mostra o trabalho em andamento, concluido ou com erro.",
                tasks.stream().map(TaskJpaEntity::getAvailability).findFirst().orElse("live"),
                "/tasks"
        ));
        facets.add(new WorkspaceFacetResponse(
                "facet-library",
                "Biblioteca",
                libraryCount + " artefatos versionados",
                libraryCount == 0
                        ? "Nenhum artefato foi encontrado na biblioteca."
                        : "Consulte arquivos, entregas e versoes registradas pelo time.",
                "live",
                "/library"
        ));
        facets.add(new WorkspaceFacetResponse(
                "facet-agents",
                "Agentes",
                threadCount + " threads persistidas",
                threadCount == 0
                        ? "Nenhuma conversa de agente foi registrada ainda."
                        : "Acompanhe conversas, execucao e falhas dos agentes.",
                threads.stream().map(AgentThreadJpaEntity::getAvailability).findFirst().orElse("live"),
                "/agents"
        ));
        facets.add(new WorkspaceFacetResponse(
                "facet-inbox",
                "Inbox",
                unreadNotifications + " notificacoes em aberto",
                unreadNotifications == 0
                        ? "Nao ha notificacoes pendentes no momento."
                        : "Eventos e alertas exibidos aqui sao gerados pelo proprio workspace.",
                "live",
                "/inbox"
        ));

        return new WorkspaceSummaryResponse(
                workspaceContextService.getWorkspaceName(),
                workspaceContextService.getOrganizationName(),
                new SummaryCountsResponse(userCount, libraryCount, threadCount, projectCount, taskCount, unreadNotifications),
                recentItems,
                facets
        );
    }

    private String buildRecentTaskDetail(TaskJpaEntity task) {
        String status = task.getStatusLabel() == null || task.getStatusLabel().isBlank()
                ? "status nao informado"
                : task.getStatusLabel();
        return status + " . " + formatMoment(task.getUpdatedAt());
    }

    private String formatMoment(LocalDateTime moment) {
        if (moment == null) {
            return "momento indisponivel";
        }
        return SUMMARY_TIME_FORMAT.format(moment);
    }

    private record RecentCandidate(LocalDateTime timestamp, RecentItemResponse item) {
    }
}
