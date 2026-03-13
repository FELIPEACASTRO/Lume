package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.ByokConnectionResponse;
import com.lume.workspace.dto.CreateByokConnectionRequest;
import com.lume.workspace.dto.UpdateByokConnectionRequest;
import com.lume.workspace.entity.WorkspaceByokConnectionJpaEntity;
import com.lume.workspace.inference.security.SecretResolver;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class WorkspaceByokConnectionService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Pattern SECRET_REF_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]{1,159}$");

    private final WorkspaceByokConnectionJpaRepository byokConnectionRepository;
    private final WorkspaceContextService workspaceContextService;
    private final ProviderCatalogService providerCatalogService;
    private final SecretResolver secretResolver;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final AuditLogService auditLogService;

    public WorkspaceByokConnectionService(
            WorkspaceByokConnectionJpaRepository byokConnectionRepository,
            WorkspaceContextService workspaceContextService,
            ProviderCatalogService providerCatalogService,
            SecretResolver secretResolver,
            WorkspaceLedgerService workspaceLedgerService,
            AuditLogService auditLogService
    ) {
        this.byokConnectionRepository = byokConnectionRepository;
        this.workspaceContextService = workspaceContextService;
        this.providerCatalogService = providerCatalogService;
        this.secretResolver = secretResolver;
        this.workspaceLedgerService = workspaceLedgerService;
        this.auditLogService = auditLogService;
    }

    public List<ByokConnectionResponse> listCurrentWorkspace() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return byokConnectionRepository.findByWorkspaceIdOrderByUpdatedAtDesc(
                workspaceContextService.getWorkspaceId(), PageRequest.of(0, 200))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ByokConnectionResponse getCurrentWorkspaceById(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return toResponse(requireCurrentWorkspaceById(id));
    }

    @Transactional
    public ByokConnectionResponse createCurrentWorkspace(CreateByokConnectionRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);

        String providerCode = providerCatalogService.normalizeProviderCode(request.providerCode());
        providerCatalogService.requireProvider(providerCode);

        String connectionName = normalizeConnectionName(request.connectionName());
        if (byokConnectionRepository.existsByWorkspaceIdAndConnectionName(workspaceContextService.getWorkspaceId(), connectionName)) {
            throw new BusinessRuleException("Ja existe uma conexao BYOK com este connectionName neste workspace.");
        }

        WorkspaceByokConnectionJpaEntity connection = new WorkspaceByokConnectionJpaEntity();
        connection.setId("byok-" + UUID.randomUUID());
        connection.setWorkspaceId(workspaceContextService.getWorkspaceId());
        connection.setProviderCode(providerCode);
        connection.setConnectionName(connectionName);
        connection.setSecretRef(normalizeSecretRef(request.secretRef()));
        connection.setScopeLabel(normalizeScopeLabel(request.scopeLabel()));
        connection.setStatus("active");
        connection.setHealthStatus("unknown");
        connection.setLastError(null);
        connection.setLastValidatedAt(null);

        WorkspaceByokConnectionJpaEntity saved = byokConnectionRepository.save(connection);
        workspaceLedgerService.recordUsageEvent(
                "byok.connection_created",
                "byok_connection",
                saved.getId(),
                "Conexao BYOK registrada para provider " + saved.getProviderCode() + "."
        );
        auditLogService.record(
                "byok_connection",
                saved.getId(),
                "created",
                Map.of(
                        "providerCode", saved.getProviderCode(),
                        "connectionName", saved.getConnectionName(),
                        "status", saved.getStatus()
                )
        );
        return toResponse(saved);
    }

    @Transactional
    public ByokConnectionResponse updateCurrentWorkspace(String id, UpdateByokConnectionRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceByokConnectionJpaEntity connection = requireCurrentWorkspaceById(id);

        if (request.connectionName() != null && !request.connectionName().isBlank()) {
            connection.setConnectionName(normalizeConnectionName(request.connectionName()));
        }
        if (request.secretRef() != null && !request.secretRef().isBlank()) {
            connection.setSecretRef(normalizeSecretRef(request.secretRef()));
            connection.setHealthStatus("unknown");
            connection.setLastError(null);
            connection.setLastValidatedAt(null);
        }
        if (request.scopeLabel() != null && !request.scopeLabel().isBlank()) {
            connection.setScopeLabel(normalizeScopeLabel(request.scopeLabel()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            connection.setStatus(normalizeStatus(request.status()));
        }

        WorkspaceByokConnectionJpaEntity saved = byokConnectionRepository.save(connection);
        auditLogService.record(
                "byok_connection",
                saved.getId(),
                "updated",
                Map.of(
                        "providerCode", saved.getProviderCode(),
                        "connectionName", saved.getConnectionName(),
                        "status", saved.getStatus(),
                        "healthStatus", saved.getHealthStatus()
                )
        );
        return toResponse(saved);
    }

    @Transactional
    public ByokConnectionResponse validateCurrentWorkspace(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceByokConnectionJpaEntity connection = requireCurrentWorkspaceById(id);

        String secret = secretResolver.resolveOptional(connection.getSecretRef());
        connection.setLastValidatedAt(LocalDateTime.now());
        if (secret == null || secret.isBlank()) {
            connection.setHealthStatus("unhealthy");
            connection.setLastError("Segredo BYOK nao encontrado para secretRef informado.");
        } else {
            connection.setHealthStatus("healthy");
            connection.setLastError(null);
        }

        WorkspaceByokConnectionJpaEntity saved = byokConnectionRepository.save(connection);
        workspaceLedgerService.recordUsageEvent(
                "byok.connection_validated",
                "byok_connection",
                saved.getId(),
                "Validacao BYOK concluida com status " + saved.getHealthStatus() + "."
        );
        auditLogService.record(
                "byok_connection",
                saved.getId(),
                "validated",
                Map.of(
                        "providerCode", saved.getProviderCode(),
                        "healthStatus", saved.getHealthStatus(),
                        "status", saved.getStatus()
                )
        );
        return toResponse(saved);
    }

    private WorkspaceByokConnectionJpaEntity requireCurrentWorkspaceById(String id) {
        return byokConnectionRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("ByokConnection", id));
    }

    private ByokConnectionResponse toResponse(WorkspaceByokConnectionJpaEntity item) {
        String providerName = providerCatalogService.findProvider(item.getProviderCode())
                .map(provider -> provider.name())
                .orElse(item.getProviderCode());
        return new ByokConnectionResponse(
                item.getId(),
                item.getProviderCode(),
                providerName,
                item.getConnectionName(),
                item.getSecretRef(),
                item.getScopeLabel(),
                item.getStatus(),
                item.getHealthStatus(),
                item.getLastValidatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getLastValidatedAt()),
                item.getLastError(),
                item.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getCreatedAt()),
                item.getUpdatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getUpdatedAt())
        );
    }

    private String normalizeConnectionName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new BusinessRuleException("connectionName da conexao BYOK e obrigatorio.");
        }
        if (trimmed.length() > 160) {
            throw new BusinessRuleException("connectionName aceita ate 160 caracteres.");
        }
        return trimmed;
    }

    private String normalizeSecretRef(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!SECRET_REF_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException("secretRef deve usar padrao de env var (ex.: OPENAI_API_KEY).");
        }
        return normalized;
    }

    private String normalizeScopeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "workspace";
        }
        String normalized = token(value);
        return switch (normalized) {
            case "workspace", "organization" -> normalized;
            default -> throw new BusinessRuleException("scopeLabel deve ser workspace ou organization.");
        };
    }

    private String normalizeStatus(String value) {
        String normalized = token(value);
        return switch (normalized) {
            case "active", "disabled" -> normalized;
            default -> throw new BusinessRuleException("status da conexao BYOK deve ser active ou disabled.");
        };
    }

    private String token(String value) {
        return NormalizationUtils.token(value);
    }
}
