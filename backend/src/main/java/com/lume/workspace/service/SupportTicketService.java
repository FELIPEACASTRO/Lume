package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CreateSupportTicketRequest;
import com.lume.workspace.dto.SupportTicketResponse;
import com.lume.workspace.dto.UpdateSupportTicketRequest;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupportTicketService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SupportTicketJpaRepository supportTicketRepository;
    private final WorkspaceContextService workspaceContextService;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final AuditLogService auditLogService;
    private final SupportTicketSlaService supportTicketSlaService;

    public SupportTicketService(
            SupportTicketJpaRepository supportTicketRepository,
            WorkspaceContextService workspaceContextService,
            WorkspaceLedgerService workspaceLedgerService,
            AuditLogService auditLogService,
            SupportTicketSlaService supportTicketSlaService
    ) {
        this.supportTicketRepository = supportTicketRepository;
        this.workspaceContextService = workspaceContextService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.auditLogService = auditLogService;
        this.supportTicketSlaService = supportTicketSlaService;
    }

    public List<SupportTicketResponse> listCurrentWorkspace(int limit) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return supportTicketRepository.findByWorkspaceIdOrderByUpdatedAtDesc(
                workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SupportTicketResponse getCurrentWorkspaceTicket(String id) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return toResponse(requireCurrentWorkspaceTicket(id));
    }

    @Transactional
    public SupportTicketResponse createCurrentWorkspace(CreateSupportTicketRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        SupportTicketJpaEntity ticket = new SupportTicketJpaEntity();
        ticket.setId("ticket-" + UUID.randomUUID());
        ticket.setWorkspaceId(workspaceContextService.getWorkspaceId());
        ticket.setCreatedByUserId(workspaceContextService.getActorUserIdOrNull());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(normalizeCategory(request.category()));
        ticket.setSeverity(normalizeSeverity(request.severity()));
        ticket.setStatus("open");
        ticket.setResolutionNote(null);

        SupportTicketJpaEntity saved = supportTicketRepository.save(ticket);
        workspaceLedgerService.recordUsageEvent(
                "support.ticket_created",
                "support_ticket",
                saved.getId(),
                "Ticket aberto para operacao do workspace."
        );
        auditLogService.record(
                "support_ticket",
                saved.getId(),
                "created",
                Map.of(
                        "category", saved.getCategory(),
                        "severity", saved.getSeverity(),
                        "status", saved.getStatus()
                )
        );
        return toResponse(saved);
    }

    @Transactional
    public SupportTicketResponse updateCurrentWorkspace(String id, UpdateSupportTicketRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        SupportTicketJpaEntity ticket = requireCurrentWorkspaceTicket(id);

        if (request.status() != null && !request.status().isBlank()) {
            ticket.setStatus(normalizeStatus(request.status()));
        }
        if (request.severity() != null && !request.severity().isBlank()) {
            ticket.setSeverity(normalizeSeverity(request.severity()));
        }
        if (request.category() != null && !request.category().isBlank()) {
            ticket.setCategory(normalizeCategory(request.category()));
        }
        if (request.resolutionNote() != null) {
            ticket.setResolutionNote(request.resolutionNote().isBlank() ? null : request.resolutionNote().trim());
        }

        SupportTicketJpaEntity saved = supportTicketRepository.save(ticket);
        auditLogService.record(
                "support_ticket",
                saved.getId(),
                "updated",
                Map.of(
                        "category", saved.getCategory(),
                        "severity", saved.getSeverity(),
                        "status", saved.getStatus()
                )
        );
        return toResponse(saved);
    }

    private SupportTicketJpaEntity requireCurrentWorkspaceTicket(String id) {
        return supportTicketRepository.findByIdAndWorkspaceId(id, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", id));
    }

    private SupportTicketResponse toResponse(SupportTicketJpaEntity item) {
        return new SupportTicketResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getSeverity(),
                item.getStatus(),
                item.getResolutionNote(),
                item.getCreatedByUserId(),
                supportTicketSlaService.resolveTargetAt(item) == null ? null : DATE_TIME_FORMATTER.format(supportTicketSlaService.resolveTargetAt(item)),
                supportTicketSlaService.isBreached(item),
                item.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getCreatedAt()),
                item.getUpdatedAt() == null ? null : DATE_TIME_FORMATTER.format(item.getUpdatedAt())
        );
    }

    private String normalizeCategory(String value) {
        if (value == null || value.isBlank()) {
            return "operational";
        }
        String normalized = token(value);
        return switch (normalized) {
            case "operational", "billing", "provider", "security", "bug", "feature_request" -> normalized;
            default -> throw new BusinessRuleException("category do ticket deve ser operational, billing, provider, security, bug ou feature_request.");
        };
    }

    private String normalizeSeverity(String value) {
        if (value == null || value.isBlank()) {
            return "medium";
        }
        String normalized = token(value);
        return switch (normalized) {
            case "low", "medium", "high", "critical" -> normalized;
            default -> throw new BusinessRuleException("severity do ticket deve ser low, medium, high ou critical.");
        };
    }

    private String normalizeStatus(String value) {
        String normalized = token(value);
        return switch (normalized) {
            case "open", "in_progress", "resolved", "closed" -> normalized;
            default -> throw new BusinessRuleException("status do ticket deve ser open, in_progress, resolved ou closed.");
        };
    }

    private int safeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return 20;
        }
        return Math.min(requestedLimit, 200);
    }

    private String token(String value) {
        return NormalizationUtils.token(value);
    }
}
