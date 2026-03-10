package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateHomeOverviewBlockRequest;
import com.lume.workspace.dto.HomeOverviewBlockResponse;
import com.lume.workspace.dto.HomeOverviewCatalogResponse;
import com.lume.workspace.dto.HomeOverviewSettingsResponse;
import com.lume.workspace.dto.UpdateHomeOverviewBlockRequest;
import com.lume.workspace.dto.UpdateHomeOverviewSettingsRequest;
import com.lume.workspace.entity.HomeOverviewBlockJpaEntity;
import com.lume.workspace.entity.HomeOverviewSettingsJpaEntity;
import com.lume.workspace.repository.HomeOverviewBlockJpaRepository;
import com.lume.workspace.repository.HomeOverviewSettingsJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class HomeCatalogService {

    private static final String DEFAULT_SETTINGS_ID = "default";
    private static final int DEFAULT_MAX_ITEMS = 4;
    private static final int MIN_MAX_ITEMS = 1;
    private static final int MAX_MAX_ITEMS = 12;
    private static final Pattern CATALOG_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,63}$");
    private static final Pattern BLOCK_TYPE_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{1,63}$");
    private static final Pattern INTERNAL_PATH_PATTERN = Pattern.compile("^/[a-z0-9/_?=&-]*$");
    private static final Set<String> SUPPORTED_BLOCK_TYPES = Set.of(
            "in_progress",
            "alerts",
            "team_context",
            "recent",
            "quick_links"
    );

    private final HomeOverviewSettingsJpaRepository settingsRepository;
    private final HomeOverviewBlockJpaRepository blockRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;

    public HomeCatalogService(
            HomeOverviewSettingsJpaRepository settingsRepository,
            HomeOverviewBlockJpaRepository blockRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.settingsRepository = settingsRepository;
        this.blockRepository = blockRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public HomeOverviewCatalogResponse getCatalog() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return new HomeOverviewCatalogResponse(
                toSettingsResponse(getOrCreateSettings()),
                blockRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(this::toBlockResponse)
                        .toList()
        );
    }

    public HomeOverviewSettingsResponse getSettings() {
        return toSettingsResponse(getOrCreateSettings());
    }

    public List<HomeOverviewBlockResponse> listBlocks() {
        return blockRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toBlockResponse)
                .toList();
    }

    @Transactional
    public HomeOverviewSettingsResponse updateSettings(UpdateHomeOverviewSettingsRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        HomeOverviewSettingsJpaEntity settings = getOrCreateSettings();
        if (request.headline() != null) {
            settings.setHeadline(validRequiredText(request.headline(), "headline", 180));
        }
        if (request.supportingText() != null) {
            settings.setSupportingText(validRequiredText(request.supportingText(), "supportingText", 255));
        }
        HomeOverviewSettingsJpaEntity saved = settingsRepository.save(settings);
        auditLogService.record(
                "home_overview_settings",
                saved.getId(),
                "updated",
                Map.of("headline", saved.getHeadline(), "supportingText", saved.getSupportingText())
        );
        return toSettingsResponse(saved);
    }

    @Transactional
    public HomeOverviewBlockResponse createBlock(CreateHomeOverviewBlockRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        String id = validCatalogKey(request.id(), "id");
        if (blockRepository.existsById(id)) {
            throw new IllegalArgumentException("id ja existe nos blocos da home.");
        }
        String blockType = validBlockType(request.blockType());
        if (blockRepository.findByBlockTypeIgnoreCase(blockType).isPresent()) {
            throw new IllegalArgumentException("blockType ja existe nos blocos da home.");
        }
        HomeOverviewBlockJpaEntity entity = new HomeOverviewBlockJpaEntity();
        entity.setId(id);
        entity.setBlockType(blockType);
        entity.setTitle(validRequiredText(request.title(), "title", 120));
        entity.setDescription(validRequiredText(request.description(), "description", 255));
        entity.setSortOrder(validSortOrder(request.sortOrder()));
        entity.setMaxItems(validMaxItems(request.maxItems(), blockType));
        applyBlockAction(entity, request.ctaLabel(), request.ctaPath());
        entity.setEnabled(request.enabled() == null || request.enabled());
        HomeOverviewBlockJpaEntity saved = blockRepository.save(entity);
        auditLogService.record(
                "home_overview_block",
                saved.getId(),
                "created",
                Map.of(
                        "blockType", saved.getBlockType(),
                        "title", saved.getTitle(),
                        "maxItems", saved.getMaxItems(),
                        "ctaLabel", saved.getCtaLabel() == null ? "" : saved.getCtaLabel(),
                        "ctaPath", saved.getCtaPath() == null ? "" : saved.getCtaPath()
                )
        );
        return toBlockResponse(saved);
    }

    @Transactional
    public HomeOverviewBlockResponse updateBlock(String id, UpdateHomeOverviewBlockRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        HomeOverviewBlockJpaEntity block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloco da home", id));
        if (request.title() != null) {
            block.setTitle(validRequiredText(request.title(), "title", 120));
        }
        if (request.description() != null) {
            block.setDescription(validRequiredText(request.description(), "description", 255));
        }
        if (request.sortOrder() != null) {
            block.setSortOrder(validSortOrder(request.sortOrder()));
        }
        if (request.maxItems() != null) {
            block.setMaxItems(validMaxItems(request.maxItems(), block.getBlockType()));
        }
        if (request.ctaLabel() != null || request.ctaPath() != null) {
            applyBlockAction(block, request.ctaLabel(), request.ctaPath());
        }
        if (request.enabled() != null) {
            block.setEnabled(request.enabled());
        }
        HomeOverviewBlockJpaEntity saved = blockRepository.save(block);
        auditLogService.record(
                "home_overview_block",
                saved.getId(),
                "updated",
                Map.of(
                        "title", saved.getTitle(),
                        "description", saved.getDescription(),
                        "sortOrder", saved.getSortOrder(),
                        "maxItems", saved.getMaxItems(),
                        "ctaLabel", saved.getCtaLabel() == null ? "" : saved.getCtaLabel(),
                        "ctaPath", saved.getCtaPath() == null ? "" : saved.getCtaPath(),
                        "enabled", saved.isEnabled()
                )
        );
        return toBlockResponse(saved);
    }

    @Transactional
    public void deleteBlock(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        HomeOverviewBlockJpaEntity block = blockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloco da home", id));
        if (blockRepository.count() <= 1) {
            throw new IllegalArgumentException("A home precisa manter pelo menos um bloco.");
        }
        auditLogService.record(
                "home_overview_block",
                block.getId(),
                "deleted",
                Map.of("blockType", block.getBlockType(), "title", block.getTitle())
        );
        blockRepository.delete(block);
    }

    private HomeOverviewSettingsJpaEntity getOrCreateSettings() {
        return settingsRepository.findById(DEFAULT_SETTINGS_ID).orElseGet(() -> {
            HomeOverviewSettingsJpaEntity entity = new HomeOverviewSettingsJpaEntity();
            entity.setId(DEFAULT_SETTINGS_ID);
            entity.setHeadline("O que voce quer fazer?");
            entity.setSupportingText("Busque informacoes do workspace, abra uma nova tarefa e acompanhe o que pede atencao.");
            return settingsRepository.save(entity);
        });
    }

    private HomeOverviewSettingsResponse toSettingsResponse(HomeOverviewSettingsJpaEntity settings) {
        return new HomeOverviewSettingsResponse(settings.getHeadline(), settings.getSupportingText());
    }

    private HomeOverviewBlockResponse toBlockResponse(HomeOverviewBlockJpaEntity block) {
        return new HomeOverviewBlockResponse(
                block.getId(),
                block.getBlockType(),
                block.getTitle(),
                block.getDescription(),
                block.getSortOrder(),
                block.getMaxItems(),
                block.getCtaLabel(),
                block.getCtaPath(),
                block.isEnabled()
        );
    }

    private String validBlockType(String blockType) {
        String normalized = blockType == null ? "" : blockType.trim().toLowerCase().replace('-', '_');
        if (!BLOCK_TYPE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("blockType deve usar letras minusculas, numeros, hifen ou underscore.");
        }
        if (!SUPPORTED_BLOCK_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("blockType deve ser um dos blocos suportados da home.");
        }
        return normalized;
    }

    private String validCatalogKey(String value, String field) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (!CATALOG_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " deve usar letras minusculas, numeros e hifen.");
        }
        return normalized;
    }

    private int validSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder deve ser um numero inteiro maior ou igual a zero.");
        }
        return sortOrder;
    }

    private int validMaxItems(Integer maxItems, String blockType) {
        if (maxItems == null) {
            return defaultMaxItems(blockType);
        }
        if (maxItems < MIN_MAX_ITEMS || maxItems > MAX_MAX_ITEMS) {
            throw new IllegalArgumentException("maxItems deve ficar entre 1 e 12.");
        }
        return maxItems;
    }

    private int defaultMaxItems(String blockType) {
        if ("quick_links".equals(blockType)) {
            return 6;
        }
        return DEFAULT_MAX_ITEMS;
    }

    private void applyBlockAction(HomeOverviewBlockJpaEntity block, String ctaLabel, String ctaPath) {
        String normalizedLabel = normalizeOptionalText(ctaLabel, "ctaLabel", 64);
        String normalizedPath = normalizeOptionalPath(ctaPath);
        if ((normalizedLabel == null) != (normalizedPath == null)) {
            throw new IllegalArgumentException("ctaLabel e ctaPath precisam ser informados juntos.");
        }
        block.setCtaLabel(normalizedLabel);
        block.setCtaPath(normalizedPath);
    }

    private String validRequiredText(String value, String field, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " nao pode ficar vazio.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " excede o tamanho maximo suportado.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String field, int maxLength) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " excede o tamanho maximo suportado.");
        }
        return normalized;
    }

    private String normalizeOptionalPath(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("ctaPath excede o tamanho maximo suportado.");
        }
        if (!INTERNAL_PATH_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("ctaPath deve apontar para uma rota interna iniciando com '/'.");
        }
        return normalized;
    }
}
