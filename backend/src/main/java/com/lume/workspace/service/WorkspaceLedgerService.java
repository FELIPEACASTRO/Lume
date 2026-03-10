package com.lume.workspace.service;

import com.lume.workspace.dto.CostLedgerEntryResponse;
import com.lume.workspace.dto.CreditLedgerEntryResponse;
import com.lume.workspace.dto.FinopsScorecardResponse;
import com.lume.workspace.dto.PolicyDecisionSummaryResponse;
import com.lume.workspace.dto.UsageEventResponse;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.entity.WorkspaceCostLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceCreditLedgerEntryJpaEntity;
import com.lume.workspace.entity.WorkspaceUsageEventJpaEntity;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.WorkspaceCostLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import com.lume.workspace.repository.WorkspaceUsageEventJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WorkspaceLedgerService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String STATUS_COMPLETED = "completed";

    private final WorkspaceUsageEventJpaRepository usageEventRepository;
    private final WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;
    private final WorkspaceCostLedgerEntryJpaRepository costLedgerRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final TaskJpaRepository taskRepository;
    private final WorkspaceContextService workspaceContextService;

    public WorkspaceLedgerService(
            WorkspaceUsageEventJpaRepository usageEventRepository,
            WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository,
            WorkspaceCostLedgerEntryJpaRepository costLedgerRepository,
            WorkspaceJpaRepository workspaceRepository,
            TaskJpaRepository taskRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.usageEventRepository = usageEventRepository;
        this.creditLedgerRepository = creditLedgerRepository;
        this.costLedgerRepository = costLedgerRepository;
        this.workspaceRepository = workspaceRepository;
        this.taskRepository = taskRepository;
        this.workspaceContextService = workspaceContextService;
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
        return usageEventRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .limit(safeLimit(limit))
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
        return creditLedgerRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .limit(safeLimit(limit))
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
        return costLedgerRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .limit(safeLimit(limit))
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

        long totalTasks = taskRepository.countByWorkspaceId(workspaceId);
        long completedTasks = taskRepository.countByWorkspaceIdAndRuntimeState(workspaceId, STATUS_COMPLETED);
        Double taskCompletionRate = totalTasks == 0 ? null : ratio(completedTasks, totalTasks);

        long totalRuns = costLedgerRepository.countByWorkspaceId(workspaceId);
        long completedRuns = costLedgerRepository.countByWorkspaceIdAndStatus(workspaceId, STATUS_COMPLETED);
        Double inferenceSuccessRate = totalRuns == 0 ? null : ratio(completedRuns, totalRuns);

        Double averageCostUsd = costLedgerRepository.averageEstimatedCostByWorkspaceId(workspaceId);
        int currentCreditBalance = creditLedgerRepository.sumCreditsByWorkspaceId(workspaceId);

        return new FinopsScorecardResponse(
                activationMinutes,
                null,
                null,
                null,
                null,
                taskCompletionRate,
                inferenceSuccessRate,
                averageCostUsd == null || averageCostUsd == 0D ? null : averageCostUsd,
                currentCreditBalance,
                "D30, churn, margem de contribuicao e NPS operacional dependem de trilhas analiticas/comerciais que ainda estao em maturacao."
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
