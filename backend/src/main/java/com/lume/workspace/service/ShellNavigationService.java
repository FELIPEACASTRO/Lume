package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.ShellCatalogItemResponse;
import com.lume.workspace.dto.ShellCatalogResponse;
import com.lume.workspace.dto.ShellCatalogTaskTypeResponse;
import com.lume.workspace.dto.CreateShellNavigationItemRequest;
import com.lume.workspace.dto.CreateShellTaskTypeRequest;
import com.lume.workspace.dto.ShellNavigationItemResponse;
import com.lume.workspace.dto.ShellNavigationResponse;
import com.lume.workspace.dto.ShellTaskTypeResponse;
import com.lume.workspace.dto.UpdateShellNavigationItemRequest;
import com.lume.workspace.dto.UpdateShellTaskTypeRequest;
import com.lume.workspace.entity.ShellNavigationItemJpaEntity;
import com.lume.workspace.entity.ShellTaskTypeJpaEntity;
import com.lume.workspace.repository.ShellNavigationItemJpaRepository;
import com.lume.workspace.repository.ShellTaskTypeJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ShellNavigationService {

    private static final Set<String> ALLOWED_ICON_KEYS = Set.of(
            "home",
            "users",
            "agents",
            "library",
            "projects",
            "tasks",
            "search",
            "inbox",
            "usage",
            "settings",
            "chat",
            "prompts",
            "billing",
            "admin",
            "help"
    );

    private static final Set<String> ALLOWED_AVAILABILITY_VALUES = Set.of(
            "live",
            "attention",
            "unavailable",
            "restricted"
    );

    private static final Set<String> ALLOWED_GROUPS = Set.of(
            "primary",
            "task-history",
            "secondary"
    );

    private static final Set<String> PROTECTED_NAVIGATION_IDS = Set.of(
            "home",
            "tasks",
            "projects",
            "library",
            "users",
            "settings"
    );

    private static final Pattern CATALOG_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,63}$");

    private final WorkspaceContextService workspaceContextService;
    private final ShellNavigationItemJpaRepository shellNavigationItemRepository;
    private final ShellTaskTypeJpaRepository shellTaskTypeRepository;
    private final AuditLogService auditLogService;

    public ShellNavigationService(
            WorkspaceContextService workspaceContextService,
            ShellNavigationItemJpaRepository shellNavigationItemRepository,
            ShellTaskTypeJpaRepository shellTaskTypeRepository,
            AuditLogService auditLogService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.shellNavigationItemRepository = shellNavigationItemRepository;
        this.shellTaskTypeRepository = shellTaskTypeRepository;
        this.auditLogService = auditLogService;
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

    public ShellCatalogResponse getCatalog() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return new ShellCatalogResponse(
                shellNavigationItemRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(this::toCatalogResponse)
                        .toList(),
                shellTaskTypeRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(this::toCatalogResponse)
                        .toList()
        );
    }

    @Transactional
    public ShellCatalogItemResponse createNavigationItem(CreateShellNavigationItemRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        String id = validCatalogKey(request.id(), "id");
        if (shellNavigationItemRepository.existsById(id)) {
            throw new IllegalArgumentException("id ja existe no catalogo da shell.");
        }

        String path = validPath(request.path());
        ensurePathUnique(path, null);

        ShellNavigationItemJpaEntity item = new ShellNavigationItemJpaEntity();
        item.setId(id);
        item.setLabel(requiredText(request.label(), "label"));
        item.setPath(path);
        item.setDescription(requiredText(request.description(), "description"));
        item.setIcon(validIcon(request.icon()));
        item.setAvailability(validAvailability(request.availability()));
        item.setNavGroup(validGroup(request.group()));
        item.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        item.setEnabled(request.enabled() == null || request.enabled());
        item.setKeywordsRaw(String.join(", ", normalizeKeywords(request.keywords() == null ? List.of() : request.keywords())));

        ShellNavigationItemJpaEntity savedItem = shellNavigationItemRepository.save(item);
        auditLogService.record(
                "shell_navigation_item",
                savedItem.getId(),
                "created",
                Map.of(
                        "path", savedItem.getPath(),
                        "navGroup", savedItem.getNavGroup(),
                        "availability", savedItem.getAvailability(),
                        "sortOrder", savedItem.getSortOrder(),
                        "enabled", savedItem.isEnabled()
                )
        );
        return toCatalogResponse(savedItem);
    }

    @Transactional
    public ShellCatalogItemResponse updateNavigationItem(String id, UpdateShellNavigationItemRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        ShellNavigationItemJpaEntity item = shellNavigationItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShellNavigationItem", id));

        if (request.label() != null) {
            item.setLabel(requiredText(request.label(), "label"));
        }
        if (request.path() != null) {
            String path = validPath(request.path());
            ensurePathUnique(path, item.getId());
            item.setPath(path);
        }
        if (request.description() != null) {
            item.setDescription(requiredText(request.description(), "description"));
        }
        if (request.icon() != null) {
            item.setIcon(validIcon(request.icon()));
        }
        if (request.availability() != null) {
            item.setAvailability(validAvailability(request.availability()));
        }
        if (request.group() != null) {
            item.setNavGroup(validGroup(request.group()));
        }
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.enabled() != null) {
            item.setEnabled(request.enabled());
        }
        if (request.keywords() != null) {
            item.setKeywordsRaw(String.join(", ", normalizeKeywords(request.keywords())));
        }

        ShellNavigationItemJpaEntity savedItem = shellNavigationItemRepository.save(item);
        auditLogService.record(
                "shell_navigation_item",
                savedItem.getId(),
                "updated",
                Map.of(
                        "path", savedItem.getPath(),
                        "navGroup", savedItem.getNavGroup(),
                        "availability", savedItem.getAvailability(),
                        "sortOrder", savedItem.getSortOrder(),
                        "enabled", savedItem.isEnabled()
                )
        );
        return toCatalogResponse(savedItem);
    }

    @Transactional
    public void deleteNavigationItem(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        ShellNavigationItemJpaEntity item = shellNavigationItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShellNavigationItem", id));
        if (PROTECTED_NAVIGATION_IDS.contains(item.getId())) {
            throw new IllegalArgumentException("Esta area faz parte da navegacao essencial e nao pode ser removida.");
        }
        shellNavigationItemRepository.delete(item);
        auditLogService.record(
                "shell_navigation_item",
                item.getId(),
                "deleted",
                Map.of(
                        "path", item.getPath(),
                        "navGroup", item.getNavGroup()
                )
        );
    }

    @Transactional
    public ShellCatalogTaskTypeResponse createTaskType(CreateShellTaskTypeRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        String taskType = validCatalogKey(request.taskType(), "taskType");
        if (shellTaskTypeRepository.existsById(taskType)) {
            throw new IllegalArgumentException("taskType ja existe no catalogo da shell.");
        }

        ShellTaskTypeJpaEntity item = new ShellTaskTypeJpaEntity();
        item.setTaskType(taskType);
        item.setLabel(requiredText(request.label(), "label"));
        item.setDescription(requiredText(request.description(), "description"));
        item.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        item.setEnabled(request.enabled() == null || request.enabled());

        ShellTaskTypeJpaEntity savedItem = shellTaskTypeRepository.save(item);
        auditLogService.record(
                "shell_task_type",
                savedItem.getTaskType(),
                "created",
                Map.of(
                        "label", savedItem.getLabel(),
                        "sortOrder", savedItem.getSortOrder(),
                        "enabled", savedItem.isEnabled()
                )
        );
        return toCatalogResponse(savedItem);
    }

    @Transactional
    public ShellCatalogTaskTypeResponse updateTaskType(String taskType, UpdateShellTaskTypeRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        ShellTaskTypeJpaEntity item = shellTaskTypeRepository.findById(taskType)
                .orElseThrow(() -> new ResourceNotFoundException("ShellTaskType", taskType));

        if (request.label() != null) {
            item.setLabel(requiredText(request.label(), "label"));
        }
        if (request.description() != null) {
            item.setDescription(requiredText(request.description(), "description"));
        }
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.enabled() != null) {
            item.setEnabled(request.enabled());
        }

        ShellTaskTypeJpaEntity savedItem = shellTaskTypeRepository.save(item);
        auditLogService.record(
                "shell_task_type",
                savedItem.getTaskType(),
                "updated",
                Map.of(
                        "label", savedItem.getLabel(),
                        "sortOrder", savedItem.getSortOrder(),
                        "enabled", savedItem.isEnabled()
                )
        );
        return toCatalogResponse(savedItem);
    }

    @Transactional
    public void deleteTaskType(String taskType) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        ShellTaskTypeJpaEntity item = shellTaskTypeRepository.findById(taskType)
                .orElseThrow(() -> new ResourceNotFoundException("ShellTaskType", taskType));
        if (shellTaskTypeRepository.count() <= 1) {
            throw new IllegalArgumentException("O catalogo precisa manter pelo menos um tipo de tarefa.");
        }
        shellTaskTypeRepository.delete(item);
        auditLogService.record(
                "shell_task_type",
                item.getTaskType(),
                "deleted",
                Map.of("label", item.getLabel())
        );
    }

    private boolean isVisible(ShellNavigationItemJpaEntity item, List<String> permissions) {
        if ("users".equals(item.getId())) {
            return permissions.contains(WorkspaceContextService.PERMISSION_MEMBERS_READ);
        }
        if ("usage".equals(item.getId())) {
            return permissions.contains(WorkspaceContextService.PERMISSION_BUDGETS_READ);
        }
        if ("admin".equals(item.getId())) {
            return permissions.contains(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE)
                    || permissions.contains(WorkspaceContextService.PERMISSION_BUDGETS_READ);
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

    private ShellCatalogItemResponse toCatalogResponse(ShellNavigationItemJpaEntity item) {
        return new ShellCatalogItemResponse(
                item.getId(),
                item.getLabel(),
                item.getPath(),
                item.getDescription(),
                item.getIcon(),
                item.getAvailability(),
                item.getNavGroup(),
                item.getSortOrder(),
                item.isEnabled(),
                parseKeywords(item.getKeywordsRaw())
        );
    }

    private ShellCatalogTaskTypeResponse toCatalogResponse(ShellTaskTypeJpaEntity item) {
        return new ShellCatalogTaskTypeResponse(
                item.getTaskType(),
                item.getLabel(),
                item.getDescription(),
                item.getSortOrder(),
                item.isEnabled()
        );
    }

    private List<String> parseKeywords(String rawKeywords) {
        return Stream.of(rawKeywords.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        return keywords.stream()
                .map(this::normalizeOptional)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private String requiredText(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " deve ser informado.");
        }
        return normalized;
    }

    private String validPath(String value) {
        String normalized = requiredText(value, "path");
        if (!normalized.startsWith("/")) {
            throw new IllegalArgumentException("path deve comecar com '/'.");
        }
        if (normalized.contains(" ")) {
            throw new IllegalArgumentException("path nao pode conter espacos.");
        }
        return normalized;
    }

    private String validCatalogKey(String value, String field) {
        String normalized = requiredText(value, field);
        if (!CATALOG_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " deve usar letras minusculas, numeros ou '-'.");
        }
        return normalized;
    }

    private String validIcon(String value) {
        String normalized = requiredText(value, "icon");
        if (!ALLOWED_ICON_KEYS.contains(normalized)) {
            throw new IllegalArgumentException("icon deve ser um dos valores suportados da shell.");
        }
        return normalized;
    }

    private String validAvailability(String value) {
        String normalized = requiredText(value, "availability");
        if (!ALLOWED_AVAILABILITY_VALUES.contains(normalized)) {
            throw new IllegalArgumentException("availability invalida para a shell.");
        }
        return normalized;
    }

    private String validGroup(String value) {
        String normalized = requiredText(value, "group");
        if (!ALLOWED_GROUPS.contains(normalized)) {
            throw new IllegalArgumentException("group invalido para a shell.");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private void ensurePathUnique(String path, String existingId) {
        shellNavigationItemRepository.findByPathIgnoreCase(path)
                .filter(item -> existingId == null || !item.getId().equals(existingId))
                .ifPresent(item -> {
                    throw new IllegalArgumentException("path ja esta em uso por outra area da shell.");
                });
    }
}
