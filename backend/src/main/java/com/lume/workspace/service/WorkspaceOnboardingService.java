package com.lume.workspace.service;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.dto.UpdateWorkspaceOnboardingRequest;
import com.lume.workspace.dto.WorkspaceOnboardingResponse;
import com.lume.workspace.entity.WorkspaceOnboardingProfileJpaEntity;
import com.lume.workspace.repository.WorkspaceOnboardingProfileJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class WorkspaceOnboardingService {

    private final WorkspaceOnboardingProfileJpaRepository onboardingRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final WorkspaceLedgerService workspaceLedgerService;
    private final WorkspaceSubscriptionService workspaceSubscriptionService;

    public WorkspaceOnboardingService(
            WorkspaceOnboardingProfileJpaRepository onboardingRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            WorkspaceLedgerService workspaceLedgerService,
            WorkspaceSubscriptionService workspaceSubscriptionService
    ) {
        this.onboardingRepository = onboardingRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.workspaceLedgerService = workspaceLedgerService;
        this.workspaceSubscriptionService = workspaceSubscriptionService;
    }

    @Transactional
    public void initializeWorkspace(Long workspaceId, BootstrapSetupRequest request) {
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceId);
        onboarding.setPrimaryUseCase(normalizePrimaryUseCase(request.primaryUseCase()));
        onboarding.setWorkStyle(normalizeWorkStyle(request.workStyle()));
        onboarding.setActivationStatus("started");
        onboarding.setActivationNote("Onboarding iniciado. Defina seu perfil para continuar.");
        onboardingRepository.save(onboarding);

        workspaceSubscriptionService.initializeSubscription(workspaceId, request.selectedPlan());
    }

    public WorkspaceOnboardingResponse getCurrentOnboarding() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        return toOnboardingResponse(getOrCreateOnboarding(workspaceContextService.getWorkspaceId()));
    }

    @Transactional
    public WorkspaceOnboardingResponse updateCurrentOnboarding(UpdateWorkspaceOnboardingRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceContextService.getWorkspaceId());
        String currentStatus = normalizeActivationStatus(onboarding.getActivationStatus());
        String targetStatus = currentStatus;
        boolean profileUpdated = false;

        if (request.primaryUseCase() != null && !request.primaryUseCase().isBlank()) {
            onboarding.setPrimaryUseCase(normalizePrimaryUseCase(request.primaryUseCase()));
            profileUpdated = true;
        }
        if (request.workStyle() != null && !request.workStyle().isBlank()) {
            onboarding.setWorkStyle(normalizeWorkStyle(request.workStyle()));
            profileUpdated = true;
        }
        if (request.activationStatus() != null && !request.activationStatus().isBlank()) {
            targetStatus = normalizeActivationStatus(request.activationStatus());
        } else if (profileUpdated && activationRank(currentStatus) < activationRank("profile_selected")) {
            targetStatus = "profile_selected";
        }
        if (activationRank(targetStatus) > activationRank(currentStatus)) {
            onboarding.setActivationStatus(targetStatus);
            onboarding.setActivationNote(defaultActivationNote(targetStatus));
            workspaceLedgerService.recordUsageEvent(
                    "onboarding.step_transition",
                    "workspace_onboarding",
                    String.valueOf(onboarding.getWorkspaceId()),
                    "Onboarding avancou para " + targetStatus + "."
            );
        } else {
            onboarding.setActivationStatus(currentStatus);
        }
        if (request.activationNote() != null && !request.activationNote().isBlank()) {
            onboarding.setActivationNote(request.activationNote().trim());
        }

        WorkspaceOnboardingProfileJpaEntity saved = onboardingRepository.save(onboarding);
        auditLogService.record(
                "workspace_onboarding",
                String.valueOf(saved.getWorkspaceId()),
                "updated",
                Map.of(
                        "primaryUseCase", saved.getPrimaryUseCase(),
                        "workStyle", saved.getWorkStyle(),
                        "activationStatus", saved.getActivationStatus()
                )
        );
        return toOnboardingResponse(saved);
    }

    @Transactional
    public WorkspaceOnboardingResponse advanceCurrentOnboarding(
            String activationStatus,
            String activationNote,
            String sourceEvent
    ) {
        Long workspaceId = workspaceContextService.getWorkspaceId();
        WorkspaceOnboardingProfileJpaEntity onboarding = getOrCreateOnboarding(workspaceId);
        String currentStatus = normalizeActivationStatus(onboarding.getActivationStatus());
        String targetStatus = normalizeActivationStatus(activationStatus);
        if (activationRank(targetStatus) <= activationRank(currentStatus)) {
            onboarding.setActivationStatus(currentStatus);
            WorkspaceOnboardingProfileJpaEntity savedCurrent = onboardingRepository.save(onboarding);
            return toOnboardingResponse(savedCurrent);
        }

        onboarding.setActivationStatus(targetStatus);
        onboarding.setActivationNote(
                activationNote == null || activationNote.isBlank()
                        ? defaultActivationNote(targetStatus)
                        : activationNote.trim()
        );
        WorkspaceOnboardingProfileJpaEntity saved = onboardingRepository.save(onboarding);

        workspaceLedgerService.recordUsageEvent(
                "onboarding.step_transition",
                "workspace_onboarding",
                String.valueOf(saved.getWorkspaceId()),
                "Onboarding avancou para " + targetStatus + " via " + (sourceEvent == null ? "manual" : sourceEvent) + "."
        );
        auditLogService.record(
                "workspace_onboarding",
                String.valueOf(saved.getWorkspaceId()),
                "step_transition",
                Map.of(
                        "activationStatus", saved.getActivationStatus(),
                        "sourceEvent", sourceEvent == null ? "manual" : sourceEvent
                )
        );
        return toOnboardingResponse(saved);
    }

    WorkspaceOnboardingProfileJpaEntity getOrCreateOnboarding(Long workspaceId) {
        return onboardingRepository.findByWorkspaceId(workspaceId).orElseGet(() -> {
            WorkspaceOnboardingProfileJpaEntity entity = new WorkspaceOnboardingProfileJpaEntity();
            entity.setWorkspaceId(workspaceId);
            return onboardingRepository.save(entity);
        });
    }

    WorkspaceOnboardingResponse toOnboardingResponse(WorkspaceOnboardingProfileJpaEntity onboarding) {
        String normalizedActivationStatus = normalizeActivationStatus(onboarding.getActivationStatus());
        return new WorkspaceOnboardingResponse(
                onboarding.getPrimaryUseCase(),
                onboarding.getWorkStyle(),
                normalizedActivationStatus,
                onboarding.getActivationNote() == null || onboarding.getActivationNote().isBlank()
                        ? defaultActivationNote(normalizedActivationStatus)
                        : onboarding.getActivationNote()
        );
    }

    // ── normalization helpers ─────────────────────────────────────────────

    String normalizePrimaryUseCase(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "operations", "research", "support", "content", "analysis" -> normalized;
            default -> throw new BusinessRuleException("primaryUseCase deve ser operations, research, support, content ou analysis.");
        };
    }

    String normalizeWorkStyle(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "solo_operator", "small_team", "department_team", "multi_team" -> normalized;
            default -> throw new BusinessRuleException("workStyle deve ser solo_operator, small_team, department_team ou multi_team.");
        };
    }

    String normalizeActivationStatus(String value) {
        String normalized = normalizeToken(value);
        return switch (normalized) {
            case "started", "profile_selected", "first_project_created", "first_task_created", "first_prompt_sent", "completed" -> normalized;
            case "guided_setup", "ready" -> "started";
            case "active" -> "completed";
            case "paused" -> "profile_selected";
            default -> throw new BusinessRuleException("activationStatus deve ser started, profile_selected, first_project_created, first_task_created, first_prompt_sent ou completed.");
        };
    }

    int activationRank(String activationStatus) {
        return switch (normalizeActivationStatus(activationStatus)) {
            case "started" -> 0;
            case "profile_selected" -> 1;
            case "first_project_created" -> 2;
            case "first_task_created" -> 3;
            case "first_prompt_sent" -> 4;
            case "completed" -> 5;
            default -> 0;
        };
    }

    String defaultActivationNote(String activationStatus) {
        return switch (normalizeActivationStatus(activationStatus)) {
            case "started" -> "Onboarding iniciado. Defina seu perfil para continuar.";
            case "profile_selected" -> "Perfil inicial salvo. Crie seu primeiro projeto.";
            case "first_project_created" -> "Projeto criado. Registre a primeira tarefa para avancar.";
            case "first_task_created" -> "Tarefa criada. Salve um template para padronizar o fluxo.";
            case "first_prompt_sent" -> "Template salvo. Execute uma conversa para concluir a ativacao.";
            case "completed" -> "Ativacao concluida. Workspace pronto para operacao continua.";
            default -> "Onboarding em andamento.";
        };
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
