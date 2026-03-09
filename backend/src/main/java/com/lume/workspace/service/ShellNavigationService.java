package com.lume.workspace.service;

import com.lume.workspace.dto.ShellNavigationItemResponse;
import com.lume.workspace.dto.ShellNavigationResponse;
import com.lume.workspace.dto.ShellTaskTypeResponse;
import com.lume.workspace.entity.ShellNavigationItemJpaEntity;
import com.lume.workspace.entity.ShellTaskTypeJpaEntity;
import com.lume.workspace.repository.ShellNavigationItemJpaRepository;
import com.lume.workspace.repository.ShellTaskTypeJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class ShellNavigationService {

    private final WorkspaceContextService workspaceContextService;
    private final ShellNavigationItemJpaRepository shellNavigationItemRepository;
    private final ShellTaskTypeJpaRepository shellTaskTypeRepository;

    public ShellNavigationService(
            WorkspaceContextService workspaceContextService,
            ShellNavigationItemJpaRepository shellNavigationItemRepository,
            ShellTaskTypeJpaRepository shellTaskTypeRepository
    ) {
        this.workspaceContextService = workspaceContextService;
        this.shellNavigationItemRepository = shellNavigationItemRepository;
        this.shellTaskTypeRepository = shellTaskTypeRepository;
    }

    public ShellNavigationResponse getNavigation() {
        List<String> permissions = workspaceContextService.getCurrentPermissions();
        List<ShellNavigationItemResponse> items = shellNavigationItemRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .filter(item -> isVisible(item, permissions))
                .map(this::toResponse)
                .toList();
        List<ShellTaskTypeResponse> taskTypes = shellTaskTypeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
        return new ShellNavigationResponse(items, taskTypes);
    }

    private boolean isVisible(ShellNavigationItemJpaEntity item, List<String> permissions) {
        if ("users".equals(item.getId())) {
            return permissions.contains(WorkspaceContextService.PERMISSION_MEMBERS_READ);
        }
        return true;
    }

    private ShellNavigationItemResponse toResponse(ShellNavigationItemJpaEntity item) {
        List<String> keywords = Stream.of(item.getKeywordsRaw().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        return new ShellNavigationItemResponse(
                item.getId(),
                item.getLabel(),
                item.getPath(),
                item.getDescription(),
                item.getIcon(),
                item.getAvailability(),
                item.getNavGroup(),
                keywords
        );
    }

    private ShellTaskTypeResponse toResponse(ShellTaskTypeJpaEntity item) {
        return new ShellTaskTypeResponse(item.getTaskType(), item.getLabel(), item.getDescription());
    }
}
