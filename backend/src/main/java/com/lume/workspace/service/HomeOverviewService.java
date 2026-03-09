package com.lume.workspace.service;

import com.lume.workspace.dto.HomeAlertResponse;
import com.lume.workspace.dto.HomeFocusItemResponse;
import com.lume.workspace.dto.HomeOverviewResponse;
import com.lume.workspace.dto.WorkspaceFacetResponse;
import com.lume.workspace.dto.WorkspaceSummaryResponse;
import com.lume.workspace.entity.NotificationJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HomeOverviewService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final WorkspaceSummaryService workspaceSummaryService;
    private final TaskJpaRepository taskRepository;
    private final NotificationJpaRepository notificationRepository;
    private final WorkspaceContextService workspaceContextService;

    public HomeOverviewService(
            WorkspaceSummaryService workspaceSummaryService,
            TaskJpaRepository taskRepository,
            NotificationJpaRepository notificationRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.workspaceSummaryService = workspaceSummaryService;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public HomeOverviewResponse getOverview() {
        WorkspaceSummaryResponse summary = workspaceSummaryService.getSummary();
        Long workspaceId = workspaceContextService.getWorkspaceId();

        List<HomeFocusItemResponse> inProgress = taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)
                .stream()
                .filter(task -> "running".equalsIgnoreCase(task.getRuntimeState()) || "queued".equalsIgnoreCase(task.getRuntimeState()))
                .limit(4)
                .map(this::toFocusItem)
                .toList();

        List<HomeAlertResponse> alerts = notificationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .filter(notification -> !notification.isRead())
                .limit(4)
                .map(this::toAlertResponse)
                .toList();

        List<WorkspaceFacetResponse> teamAndContext = summary.workspaceFacets().stream()
                .filter(facet -> List.of("facet-members", "facet-projects", "facet-library", "facet-inbox").contains(facet.id()))
                .toList();

        return new HomeOverviewResponse(
                summary.workspaceName(),
                summary.organizationName(),
                "Tudo o que importa no seu workspace, em um lugar.",
                "Abra tarefas, acompanhe alertas e retome o que precisa de voce.",
                inProgress,
                summary.recentItems(),
                alerts,
                teamAndContext
        );
    }

    private HomeFocusItemResponse toFocusItem(TaskJpaEntity task) {
        return new HomeFocusItemResponse(
                task.getId(),
                task.getTitle(),
                task.getSummary(),
                task.getStatusLabel(),
                task.getRuntimeState(),
                task.getOwnerName(),
                "/tasks/" + task.getId()
        );
    }

    private HomeAlertResponse toAlertResponse(NotificationJpaEntity notification) {
        return new HomeAlertResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getKind(),
                notification.getPath(),
                notification.getCreatedAt().format(DATE_TIME_FORMATTER),
                notification.isRead()
        );
    }
}
