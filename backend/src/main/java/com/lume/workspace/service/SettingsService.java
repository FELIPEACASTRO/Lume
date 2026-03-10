package com.lume.workspace.service;

import com.lume.workspace.dto.SettingsOverviewResponse;
import com.lume.workspace.dto.SettingsPreferencesResponse;
import com.lume.workspace.dto.SettingsSectionResponse;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SettingsService {

    private final WorkspaceContextService workspaceContextService;
    private final NotificationService notificationService;
    private final UsageService usageService;
    private final KnowledgeSourceService knowledgeSourceService;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final AuditLogService auditLogService;
    private final WorkspaceCommercialService workspaceCommercialService;

    public SettingsService(
            WorkspaceContextService workspaceContextService,
            NotificationService notificationService,
            UsageService usageService,
            KnowledgeSourceService knowledgeSourceService,
            UserPreferenceJpaRepository userPreferenceRepository,
            AuditLogService auditLogService,
            WorkspaceCommercialService workspaceCommercialService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.notificationService = notificationService;
        this.usageService = usageService;
        this.knowledgeSourceService = knowledgeSourceService;
        this.userPreferenceRepository = userPreferenceRepository;
        this.auditLogService = auditLogService;
        this.workspaceCommercialService = workspaceCommercialService;
    }

    public SettingsOverviewResponse getOverview() {
        List<String> permissions = workspaceContextService.getCurrentPermissions();
        List<SettingsSectionResponse> sections = new ArrayList<>(List.of(
                new SettingsSectionResponse("conta", "Perfil e preferencias", "Dados pessoais e escolhas basicas da operacao.", "live"),
                new SettingsSectionResponse("configuracoes", "Workspace", "Idioma, aparencia e avisos do workspace.", "live"),
                new SettingsSectionResponse("uso", "Uso", "Consumo, limites e leituras principais do workspace.", "live")
        ));
        if (permissions.contains(WorkspaceContextService.PERMISSION_KNOWLEDGE_READ)) {
            sections.add(new SettingsSectionResponse(
                    "knowledge",
                    "Conhecimento",
                    "Fontes, contexto e acesso ao conhecimento do workspace.",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_BUDGETS_READ)) {
            sections.add(new SettingsSectionResponse(
                    "finops",
                    "Uso e budgets",
                    "Centro de custo, limites e acompanhamento do consumo.",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_PROVIDERS_READ)) {
            sections.add(new SettingsSectionResponse(
                    "providers-runtime",
                    "Modelos e acesso",
                    "Modelos disponiveis, acesso e situacao de cada integracao.",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE)) {
            sections.add(new SettingsSectionResponse(
                    "home-overview",
                    "Tela inicial",
                    "Ajuste a mensagem principal e os blocos exibidos no inicio.",
                    "live"
            ));
            sections.add(new SettingsSectionResponse(
                    "workspace-catalog",
                    "Navegacao e tarefas",
                    "Ajuste o menu principal e os tipos de tarefa sem novo deploy.",
                    "live"
            ));
        }
        return new SettingsOverviewResponse(
                workspaceContextService.getOrganizationName(),
                workspaceContextService.getWorkspaceName(),
                workspaceContextService.getSession().role().label(),
                notificationService.unreadCount(),
                knowledgeSourceService.countSources(),
                usageService.getSummary(),
                workspaceCommercialService.getCurrentSummary(),
                getPreferences(),
                sections
        );
    }

    public SettingsPreferencesResponse getPreferences() {
        return toPreferencesResponse(getOrCreatePreferences());
    }

    @Transactional
    public SettingsPreferencesResponse updatePreferences(UpdateSettingsPreferencesRequest request) {
        UserPreferenceJpaEntity preference = getOrCreatePreferences();

        if (request.appearance() != null) {
            preference.setAppearance(normalizeAppearance(request.appearance()));
        }
        if (request.languageCode() != null && !request.languageCode().isBlank()) {
            preference.setLanguageCode(request.languageCode().trim());
        }
        if (request.emailUpdates() != null) {
            preference.setEmailUpdates(request.emailUpdates());
        }
        if (request.productUpdates() != null) {
            preference.setProductUpdates(request.productUpdates());
        }

        UserPreferenceJpaEntity savedPreference = userPreferenceRepository.save(preference);
        auditLogService.record(
                "settings_preferences",
                String.valueOf(savedPreference.getUserId()),
                "updated",
                java.util.Map.of(
                        "appearance", savedPreference.getAppearance(),
                        "languageCode", savedPreference.getLanguageCode(),
                        "emailUpdates", savedPreference.isEmailUpdates(),
                        "productUpdates", savedPreference.isProductUpdates()
                )
        );
        return toPreferencesResponse(savedPreference);
    }

    private UserPreferenceJpaEntity getOrCreatePreferences() {
        Long actorUserId = workspaceContextService.getActorUserIdOrNull();
        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(actorUserId).orElseGet(() -> {
            UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
            entity.setUserId(actorUserId);
            return entity;
        });

        preference.setAppearance(normalizeAppearance(preference.getAppearance()));
        if (preference.getLanguageCode() == null || preference.getLanguageCode().isBlank()) {
            preference.setLanguageCode("pt-BR");
        }
        return userPreferenceRepository.save(preference);
    }

    private SettingsPreferencesResponse toPreferencesResponse(UserPreferenceJpaEntity preference) {
        return new SettingsPreferencesResponse(
                normalizeAppearance(preference.getAppearance()),
                preference.getLanguageCode(),
                preference.isEmailUpdates(),
                preference.isProductUpdates()
        );
    }

    private String normalizeAppearance(String appearance) {
        if ("dark".equalsIgnoreCase(appearance)) {
            return "dark";
        }
        return "light";
    }
}
