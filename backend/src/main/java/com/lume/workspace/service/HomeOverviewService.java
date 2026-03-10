package com.lume.workspace.service;

import com.lume.workspace.dto.HomeAlertResponse;
import com.lume.workspace.dto.HomeOverviewBlockResponse;
import com.lume.workspace.dto.HomeFocusItemResponse;
import com.lume.workspace.dto.HomeOverviewResponse;
import com.lume.workspace.dto.HomeOverviewSettingsResponse;
import com.lume.workspace.dto.WorkspaceFacetResponse;
import com.lume.workspace.dto.WorkspaceSummaryResponse;
import com.lume.workspace.entity.NotificationJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HomeOverviewService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final int DEFAULT_BLOCK_LIMIT = 4;

    private final WorkspaceSummaryService workspaceSummaryService;
    private final TaskJpaRepository taskRepository;
    private final NotificationJpaRepository notificationRepository;
    private final WorkspaceContextService workspaceContextService;
    private final HomeCatalogService homeCatalogService;

    public HomeOverviewService(
            WorkspaceSummaryService workspaceSummaryService,
            TaskJpaRepository taskRepository,
            NotificationJpaRepository notificationRepository,
            WorkspaceContextService workspaceContextService,
            HomeCatalogService homeCatalogService
    ) {
        this.workspaceSummaryService = workspaceSummaryService;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        this.workspaceContextService = workspaceContextService;
        this.homeCatalogService = homeCatalogService;
    }

    public HomeOverviewResponse getOverview() {
        WorkspaceSummaryResponse summary = workspaceSummaryService.getSummary();
        Long workspaceId = workspaceContextService.getWorkspaceId();
        HomeOverviewSettingsResponse settings = homeCatalogService.getSettings();
        List<HomeOverviewBlockResponse> blocks = homeCatalogService.listBlocks();
        int inProgressLimit = blockLimit(blocks, "in_progress");
        int alertLimit = blockLimit(blocks, "alerts");
        int recentLimit = blockLimit(blocks, "recent");
        int teamContextLimit = blockLimit(blocks, "team_context");

        List<HomeFocusItemResponse> inProgress = taskRepository.findByWorkspaceIdAndRuntimeStateInOrderByUpdatedAtDesc(
                        workspaceId,
                        List.of("running", "queued", "ready"),
                        PageRequest.of(0, inProgressLimit)
                )
                .stream()
                .limit(inProgressLimit)
                .map(this::toFocusItem)
                .toList();

        List<HomeAlertResponse> alerts = notificationRepository.findByWorkspaceIdAndReadFalseOrderByCreatedAtDesc(
                        workspaceId,
                        PageRequest.of(0, alertLimit)
                )
                .stream()
                .map(this::toAlertResponse)
                .toList();

        List<WorkspaceFacetResponse> teamAndContext = summary.workspaceFacets().stream()
                .filter(facet -> List.of("facet-members", "facet-projects", "facet-library", "facet-inbox").contains(facet.id()))
                .limit(teamContextLimit)
                .toList();

        return new HomeOverviewResponse(
                summary.workspaceName(),
                summary.organizationName(),
                settings.headline(),
                settings.supportingText(),
                blocks,
                inProgress,
                summary.recentItems().stream().limit(recentLimit).toList(),
                alerts,
                teamAndContext
        );
    }

    private int blockLimit(List<HomeOverviewBlockResponse> blocks, String blockType) {
        return blocks.stream()
                .filter(block -> blockType.equals(block.blockType()))
                .map(HomeOverviewBlockResponse::maxItems)
                .findFirst()
                .orElse(DEFAULT_BLOCK_LIMIT);
    }

    private HomeFocusItemResponse toFocusItem(TaskJpaEntity task) {
        return new HomeFocusItemResponse(
                task.getId(),
                task.getTitle(),
                task.getSummary() == null || task.getSummary().isBlank() ? task.getPrompt() : task.getSummary(),
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
