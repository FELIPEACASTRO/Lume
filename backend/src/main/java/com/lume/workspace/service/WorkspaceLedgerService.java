package com.lume.workspace.service;

import com.lume.workspace.dto.CostLedgerEntryResponse;
import com.lume.workspace.dto.CreditLedgerEntryResponse;
import com.lume.workspace.dto.FinopsScorecardResponse;
import com.lume.workspace.dto.PolicyDecisionSummaryResponse;
import com.lume.workspace.dto.UsageEventResponse;
import com.lume.workspace.entity.WorkspaceFinopsReconciliationRunJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.entity.WorkspaceCostLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceCreditLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceUsageEventJpaEntity;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceFinopsReconciliationRunJpaRepository;
import com.lume.workspace.repository.WorkspaceInvoiceJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.repository.WorkspacePaymentEventJpaRepository;
import com.lume.workspace.repository.WorkspaceUsageEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceLedgerService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_PROCESSED = "processed";
    private static final String STATUS_PENDING = "pending";
    private static final List<String> OPEN_TICKET_STATUSES = List.of("open", "in_progress");

    private final WorkspaceUsageEventJpaRepository usageEventRepository;
    private final WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;
    private final WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final TaskJpaRepository taskRepository;
    private final WorkspaceInvoiceJpaRepository invoiceRepository;
    private final WorkspacePaymentEventJpaRepository paymentEventRepository;
    private final WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository;
    private final SupportTicketJpaRepository supportTicketRepository;
    private final WorkspaceByokConnectionJpaRepository byokConnectionRepository;
    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final SupportTicketSlaService supportTicketSlaService;

    public WorkspaceLedgerService(
            WorkspaceUsageEventJpaRepository usageEventRepository,
            WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository,
            WorkspaceCostLedgerEntryJpaRepository costLedgerRepository,
            WorkspaceJpaRepository workspaceRepository,
            TaskJpaRepository taskRepository,
            WorkspaceInvoiceJpaRepository invoiceRepository,
            WorkspacePaymentEventJpaRepository paymentEventRepository,
            WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository,
            SupportTicketJpaRepository supportTicketRepository,
            WorkspaceByokConnectionJpaRepository byokConnectionRepository,
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            SupportTicketSlaService supportTicketSlaService
    ) {
        this.usageEventRepository = usageEventRepository;
        this.creditLedgerRepository = creditLedgerRepository;
        this.costLedgerRepository = costLedgerRepository;
        this.workspaceRepository = workspaceRepository;
        this.taskRepository = taskRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.byokConnectionRepository = byokConnectionRepository;
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.supportTicketSlaService = supportTicketSlaService;
    }

    @Transactional
    public void recordUsageEvent(String eventType, String resourceType, String resourceId, String details) {
        recordUsageEventForWorkspace(
                workspaceContextService.getWorkspaceId(),
                workspaceContextService.getActorUserIdOrNull(),
                eventType,
                resourceType,
                resourceId,
                details
        );
    }

    @Transactional
    public void recordUsageEventForWorkspace(
            Long workspaceId,
            Long actorUserId,
            String eventType,
            String resourceType,
            String resourceId,
            String details
    ) {
        WorkspaceUsageEventJpaEntity event = new WorkspaceUsageEventJpaEntity();
        event.setWorkspaceId(workspaceId);
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setDetails(details);
        usageEventRepository.save(event);
    }

    @Transactional
    public void recordCreditEntry(
            Long workspaceId,
            String entryType,
            String sourceType,
            String sourceId,
            int creditsDelta,
            String note
    ) {
        int currentBalance = creditLedgerRepository.sumCreditsByWorkspaceId(workspaceId);
        WorkspaceCreditLedgerEntryJpaEntity entry = new WorkspaceCreditLedgerEntryJpaEntity();
        entry.setWorkspaceId(workspaceId);
        entry.setEntryType(entryType);
        entry.setSourceType(sourceType);
        entry.setSourceId(sourceId);
        entry.setCreditsDelta(creditsDelta);
        entry.setBalanceAfter(currentBalance + creditsDelta);
        entry.setNote(note);
        creditLedgerRepository.save(entry);
    }

    @Transactional
    public void recordCostEntry(CostLedgerRecord record) {
        WorkspaceCostLedgerEntryJpaEntity entry = new WorkspaceCostLedgerEntryJpaEntity();
        entry.setWorkspaceId(record.workspaceId());
        entry.setProviderCode(record.providerCode());
        entry.setModelCode(record.modelCode());
        entry.setCapability(record.capability());
        entry.setRequestId(record.requestId());
        entry.setStatus(record.status());
        entry.setEstimatedInputTokens(record.estimatedInputTokens());
        entry.setEstimatedOutputTokens(record.estimatedOutputTokens());
        entry.setEstimatedCostUsd(record.estimatedCostUsd());
        entry.setLatencyMs(record.latencyMs());
        entry.setFallbackUsed(record.fallbackUsed());
        entry.setRoutingMode(record.routingMode());
        entry.setPolicyDecisionSummary(record.policyDecisionSummary());
        costLedgerRepository.save(entry);
    }

    public List<UsageEventResponse> listUsageEvents(int limit) {
        return usageEventRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(item -> new UsageEventResponse(
                        item.getId(),
                        item.getEventType(),
                        item.getResourceType(),
                        item.getResourceId(),
                        item.getActorUserId(),
                        item.getDetails(),
                        DATE_TIME_FORMATTER.format(item.getCreatedAt())
                ))
                .toList();
    }

    public List<CreditLedgerEntryResponse> listCreditEntries(int limit) {
        return creditLedgerRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(item -> new CreditLedgerEntryResponse(
                        item.getId(),
                        item.getEntryType(),
                        item.getSourceType(),
                        item.getSourceId(),
                        item.getCreditsDelta(),
                        item.getBalanceAfter(),
                        item.getNote(),
                        DATE_TIME_FORMATTER.format(item.getCreatedAt())
                ))
                .toList();
    }

    public List<CostLedgerEntryResponse> listCostEntries(int limit) {
        return costLedgerRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId(), PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(item -> new CostLedgerEntryResponse(
                        item.getId(),
                        item.getProviderCode(),
                        item.getModelCode(),
                        item.getCapability(),
                        item.getRequestId(),
                        item.getStatus(),
                        item.getEstimatedInputTokens(),
                        item.getEstimatedOutputTokens(),
                        item.getEstimatedCostUsd(),
                        item.getLatencyMs(),
                        item.isFallbackUsed(),
                        item.getRoutingMode(),
                        new PolicyDecisionSummaryResponse(
                                item.getRoutingMode(),
                                null,
                                item.getProviderCode(),
                                item.isFallbackUsed(),
                                item.getStatus(),
                                item.getPolicyDecisionSummary()
                        ),
                        DATE_TIME_FORMATTER.format(item.getCreatedAt())
                ))
                .toList();
    }

    public FinopsScorecardResponse scorecard() {
        Long workspaceId = workspaceContextService.getWorkspaceId();
        Integer activationMinutes = activationMinutesToFirstTask(workspaceId);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long totalTasks = taskRepository.countByWorkspaceId(workspaceId);
        long completedTasks = taskRepository.countByWorkspaceIdAndRuntimeState(workspaceId, STATUS_COMPLETED);
        Double taskCompletionRate = totalTasks == 0 ? null : ratio(completedTasks, totalTasks);

        long totalRuns = costLedgerRepository.countByWorkspaceId(workspaceId);
        long completedRuns = costLedgerRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_COMPLETED);
        Double inferenceSuccessRate = totalRuns == 0 ? null : ratio(completedRuns, totalRuns);

        Double averageCostUsd = costLedgerRepository.averageEstimatedCostByWorkspaceId(workspaceId);
        long paidInvoicesCount = invoiceRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_PAID);
        long paymentFailureCount = paymentEventRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_FAILED);
        long orphanPaymentEvents = paymentEventRepository.countByWorkspaceIdAndInvoiceIsNull(workspaceId);
        long pendingPaymentEvents = paymentEventRepository.countByWorkspaceIdAndStatusAndProcessedAtIsNull(workspaceId, STATUS_PENDING);
        Optional<WorkspaceFinopsReconciliationRunJpaEntity> latestReconciliation = reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(workspaceId);
        long openSupportTickets = supportTicketRepository.countByWorkspaceIdAndStatusIn(workspaceId, OPEN_TICKET_STATUSES);
        long criticalOpenSupportTickets = supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(workspaceId, OPEN_TICKET_STATUSES, "critical");
        long overdueSupportTickets = supportTicketRepository.findByWorkspaceIdAndStatusIn(workspaceId, OPEN_TICKET_STATUSES).stream()
                .filter(supportTicketSlaService::isBreached)
                .count();
        long byokConnections = byokConnectionRepository.countByWorkspaceId(workspaceId);
        long healthyByokConnections = byokConnectionRepository.countByWorkspaceIdAndHealthStatus(workspaceId, "healthy");
        long weeklyActiveUsers = usageEventRepository.countDistinctActorUserIdByWorkspaceIdAndActorUserIdIsNotNullAndCreatedAtAfter(workspaceId, sevenDaysAgo);
        long monthlyActiveUsers = usageEventRepository.countDistinctActorUserIdByWorkspaceIdAndActorUserIdIsNotNullAndCreatedAtAfter(workspaceId, thirtyDaysAgo);
        int coreLiveProviders = 0;
        int supportedRestrictedProviders = 0;
        int blockedProviders = 0;
        for (var providerStatus : providerCatalogService.listProviderStatuses()) {
            switch (providerStatus.providerTier()) {
                case "core_live" -> coreLiveProviders++;
                case "supported_restricted" -> supportedRestrictedProviders++;
                case "blocked" -> blockedProviders++;
                default -> {
                }
            }
        }
        int currentCreditBalance = creditLedgerRepository.sumCreditsByWorkspaceId(workspaceId);

        return new FinopsScorecardResponse(
                activationMinutes,
                null,
                null,
                null,
                null,
                weeklyActiveUsers,
                monthlyActiveUsers,
                taskCompletionRate,
                inferenceSuccessRate,
                averageCostUsd == null || averageCostUsd == 0D ? null : averageCostUsd,
                paidInvoicesCount,
                paymentFailureCount,
                orphanPaymentEvents,
                pendingPaymentEvents,
                latestReconciliation.map(WorkspaceFinopsReconciliationRunJpaEntity::getReconciliationStatus).orElse(null),
                latestReconciliation.map(WorkspaceFinopsReconciliationRunJpaEntity::getCreditDrift).orElse(null),
                latestReconciliation.map(WorkspaceFinopsReconciliationRunJpaEntity::getExecutedAt)
                        .map(DATE_TIME_FORMATTER::format)
                        .orElse(null),
                openSupportTickets,
                criticalOpenSupportTickets,
                overdueSupportTickets,
                byokConnections,
                healthyByokConnections,
                coreLiveProviders,
                supportedRestrictedProviders,
                blockedProviders,
                currentCreditBalance,
                "WAU/MAU, billing, reconciliacao, suporte, BYOK e readiness ja sao medidos com dados reais. D30, churn, margem de contribuicao e NPS operacional ainda dependem de trilhas analiticas/comerciais em maturacao."
        );
    }

    private Integer activationMinutesToFirstTask(Long workspaceId) {
        var workspace = workspaceRepository.findById(workspaceId).orElse(null);
        TaskJpaEntity firstTask = taskRepository.findFirstByWorkspaceIdOrderByCreatedAtAsc(workspaceId).orElse(null);
        if (workspace == null || workspace.getCreatedAt() == null || firstTask == null || firstTask.getCreatedAt() == null) {
            return null;
        }
        long minutes = Duration.between(workspace.getCreatedAt(), firstTask.getCreatedAt()).toMinutes();
        return Math.max(0, (int) minutes);
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private int safeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return 20;
        }
        return Math.min(requestedLimit, 200);
    }

    public record CostLedgerRecord(
            Long workspaceId,
            String providerCode,
            String modelCode,
            String capability,
            String requestId,
            String status,
            Integer estimatedInputTokens,
            Integer estimatedOutputTokens,
            Double estimatedCostUsd,
            Long latencyMs,
            boolean fallbackUsed,
            String routingMode,
            String policyDecisionSummary
    ) {
    }
}
