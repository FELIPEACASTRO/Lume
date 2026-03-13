package com.lume.workspace.service;

import com.lume.workspace.dto.SettingsGovernanceSummaryResponse;
import com.lume.workspace.repository.SupportTicketJpaRepository;
import com.lume.workspace.repository.WorkspaceByokConnectionJpaRepository;
import com.lume.workspace.repository.WorkspaceFinopsReconciliationRunJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GovernanceSummaryService {

    private final SupportTicketJpaRepository supportTicketRepository;
    private final WorkspaceByokConnectionJpaRepository byokConnectionRepository;
    private final WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository;
    private final ProviderCatalogService providerCatalogService;
    private final SupportTicketSlaService supportTicketSlaService;

    public GovernanceSummaryService(
            SupportTicketJpaRepository supportTicketRepository,
            WorkspaceByokConnectionJpaRepository byokConnectionRepository,
            WorkspaceFinopsReconciliationRunJpaRepository reconciliationRunRepository,
            ProviderCatalogService providerCatalogService,
            SupportTicketSlaService supportTicketSlaService
    ) {
        this.supportTicketRepository = supportTicketRepository;
        this.byokConnectionRepository = byokConnectionRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.providerCatalogService = providerCatalogService;
        this.supportTicketSlaService = supportTicketSlaService;
    }

    public SettingsGovernanceSummaryResponse governanceSummary(Long workspaceId) {
        long openSupportTickets = supportTicketRepository.countByWorkspaceIdAndStatusIn(workspaceId, List.of("open", "in_progress"));
        long criticalOpenSupportTickets = supportTicketRepository.countByWorkspaceIdAndStatusInAndSeverity(workspaceId, List.of("open", "in_progress"), "critical");
        long overdueSupportTickets = supportTicketRepository.findByWorkspaceIdAndStatusIn(workspaceId, List.of("open", "in_progress")).stream()
                .filter(supportTicketSlaService::isBreached)
                .count();
        long byokConnections = byokConnectionRepository.countByWorkspaceId(workspaceId);
        long healthyByokConnections = byokConnectionRepository.countByWorkspaceIdAndHealthStatus(workspaceId, "healthy");
        var latestReconciliation = reconciliationRunRepository.findFirstByWorkspaceIdOrderByExecutedAtDesc(workspaceId);

        int coreLiveProviders = 0;
        int supportedRestrictedProviders = 0;
        int blockedProviders = 0;
        for (var providerStatus : providerCatalogService.listProviderStatuses()) {
            switch (providerStatus.providerTier()) {
                case "core_live" -> coreLiveProviders++;
                case "supported_restricted" -> supportedRestrictedProviders++;
                case "blocked" -> blockedProviders++;
                default -> {}
            }
        }

        String note = latestReconciliation
                .map(item -> "Ultima reconciliacao " + item.getReconciliationStatus() + " em " + item.getExecutedAt() + ".")
                .orElse("Ainda nao houve reconciliacao registrada para este workspace.");

        return new SettingsGovernanceSummaryResponse(
                openSupportTickets,
                criticalOpenSupportTickets,
                overdueSupportTickets,
                byokConnections,
                healthyByokConnections,
                coreLiveProviders,
                supportedRestrictedProviders,
                blockedProviders,
                latestReconciliation.map(item -> item.getReconciliationStatus()).orElse(null),
                latestReconciliation.map(item -> item.getExecutedAt().toString()).orElse(null),
                note
        );
    }
}
