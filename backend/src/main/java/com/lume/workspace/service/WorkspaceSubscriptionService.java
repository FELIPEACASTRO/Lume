package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.dto.UpdateWorkspaceSubscriptionRequest;
import com.lume.workspace.dto.WorkspaceSubscriptionResponse;
import com.lume.workspace.entity.WorkspaceSubscriptionJpaEntity;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
public class WorkspaceSubscriptionService {

    private final WorkspaceSubscriptionJpaRepository subscriptionRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceLedgerService workspaceLedgerService;

    public WorkspaceSubscriptionService(
            WorkspaceSubscriptionJpaRepository subscriptionRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
    }

    @Transactional
    public void initializeSubscription(Long workspaceId, String selectedPlan) {
        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceId);
        applyPlanDefaults(subscription, normalizePlanCode(selectedPlan));
        subscription.setSubscriptionStatus("active");
        subscription.setBillingInterval("monthly");
        subscription.setRenewsAt(LocalDate.now().plusDays(30));
        subscription.setCommercialNote("Plano inicial do workspace. Creditos extras e overage entram em uma trilha posterior.");
        subscriptionRepository.save(subscription);
        workspaceLedgerService.recordCreditEntry(
                workspaceId,
                "grant",
                "subscription_onboarding",
                "workspace:" + workspaceId,
                subscription.getIncludedCredits() + subscription.getExtraCredits(),
                "Creditos iniciais aplicados no bootstrap comercial do workspace."
        );
    }

    public WorkspaceSubscriptionResponse getCurrentSubscription() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return toSubscriptionResponse(getOrCreateSubscription(workspaceContextService.getWorkspaceId()));
    }

    @Transactional
    public WorkspaceSubscriptionResponse updateCurrentSubscription(UpdateWorkspaceSubscriptionRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceSubscriptionJpaEntity subscription = getOrCreateSubscription(workspaceContextService.getWorkspaceId());
        int previousTotalCredits = subscription.getIncludedCredits() + subscription.getExtraCredits();

        if (request.planCode() != null && !request.planCode().isBlank()) {
            applyPlanDefaults(subscription, normalizePlanCode(request.planCode()));
        }
        if (request.subscriptionStatus() != null && !request.subscriptionStatus().isBlank()) {
            subscription.setSubscriptionStatus(normalizeSubscriptionStatus(request.subscriptionStatus()));
        }
        if (request.billingInterval() != null && !request.billingInterval().isBlank()) {
            subscription.setBillingInterval(normalizeBillingInterval(request.billingInterval()));
        }
        if (request.includedCredits() != null) {
            subscription.setIncludedCredits(request.includedCredits());
        }
        if (request.extraCredits() != null) {
            subscription.setExtraCredits(request.extraCredits());
        }
        if (request.renewsAt() != null) {
            subscription.setRenewsAt(request.renewsAt());
        }
        if (request.commercialNote() != null && !request.commercialNote().isBlank()) {
            subscription.setCommercialNote(request.commercialNote().trim());
        }

        validateSubscription(subscription);
        WorkspaceSubscriptionJpaEntity saved = subscriptionRepository.save(subscription);
        int updatedTotalCredits = saved.getIncludedCredits() + saved.getExtraCredits();
        int deltaCredits = updatedTotalCredits - previousTotalCredits;
        if (deltaCredits != 0) {
            workspaceLedgerService.recordCreditEntry(
                    saved.getWorkspaceId(),
                    deltaCredits > 0 ? "adjustment_grant" : "adjustment_debit",
                    "subscription_update",
                    "workspace:" + saved.getWorkspaceId(),
                    deltaCredits,
                    "Ajuste de creditos apos atualizacao da assinatura."
            );
        }
        auditLogService.record(
                "workspace_subscription",
                String.valueOf(saved.getWorkspaceId()),
                "updated",
                Map.of(
                        "planCode", saved.getPlanCode(),
                        "subscriptionStatus", saved.getSubscriptionStatus(),
                        "includedCredits", saved.getIncludedCredits(),
                        "extraCredits", saved.getExtraCredits()
                )
        );
        return toSubscriptionResponse(saved);
    }

    public WorkspaceSubscriptionJpaEntity getOrCreateSubscription(Long workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId).orElseGet(() -> {
            WorkspaceSubscriptionJpaEntity entity = new WorkspaceSubscriptionJpaEntity();
            entity.setWorkspaceId(workspaceId);
            applyPlanDefaults(entity, "starter");
            entity.setRenewsAt(LocalDate.now().plusDays(30));
            return subscriptionRepository.save(entity);
        });
    }

    WorkspaceSubscriptionResponse toSubscriptionResponse(WorkspaceSubscriptionJpaEntity subscription) {
        return new WorkspaceSubscriptionResponse(
                subscription.getPlanCode(),
                subscription.getPlanLabel(),
                subscription.getSubscriptionStatus(),
                subscription.getBillingInterval(),
                subscription.getIncludedCredits(),
                subscription.getExtraCredits(),
                subscription.getIncludedCredits() + subscription.getExtraCredits(),
                subscription.getRenewsAt(),
                subscription.getCommercialNote()
        );
    }

    // ── normalization helpers ─────────────────────────────────────────────

    void applyPlanDefaults(WorkspaceSubscriptionJpaEntity subscription, String planCode) {
        switch (planCode) {
            case "core" -> {
                subscription.setPlanCode("core");
                subscription.setPlanLabel("Core");
                subscription.setIncludedCredits(1500);
            }
            case "pro" -> {
                subscription.setPlanCode("pro");
                subscription.setPlanLabel("Pro");
                subscription.setIncludedCredits(5000);
            }
            default -> {
                subscription.setPlanCode("starter");
                subscription.setPlanLabel("Starter");
                subscription.setIncludedCredits(500);
            }
        }
    }

    void validateSubscription(WorkspaceSubscriptionJpaEntity subscription) {
        if (subscription.getIncludedCredits() < 0 || subscription.getExtraCredits() < 0) {
            throw new BusinessRuleException("Os creditos comerciais nao podem ser negativos.");
        }
        normalizePlanCode(subscription.getPlanCode());
        normalizeSubscriptionStatus(subscription.getSubscriptionStatus());
        normalizeBillingInterval(subscription.getBillingInterval());
    }

    String normalizePlanCode(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "starter", "core", "pro" -> normalized;
            default -> throw new BusinessRuleException("planCode deve ser starter, core ou pro.");
        };
    }

    String normalizeSubscriptionStatus(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "active", "trialing", "past_due", "paused", "canceled" -> normalized;
            default -> throw new BusinessRuleException("subscriptionStatus deve ser active, trialing, past_due, paused ou canceled.");
        };
    }

    String normalizeBillingInterval(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "monthly", "annual" -> normalized;
            default -> throw new BusinessRuleException("billingInterval deve ser monthly ou annual.");
        };
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
