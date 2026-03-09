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

    public SettingsService(
            WorkspaceContextService workspaceContextService,
            NotificationService notificationService,
            UsageService usageService,
            KnowledgeSourceService knowledgeSourceService,
            UserPreferenceJpaRepository userPreferenceRepository,
            AuditLogService auditLogService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.notificationService = notificationService;
        this.usageService = usageService;
        this.knowledgeSourceService = knowledgeSourceService;
        this.userPreferenceRepository = userPreferenceRepository;
        this.auditLogService = auditLogService;
    }

    public SettingsOverviewResponse getOverview() {
        List<String> permissions = workspaceContextService.getCurrentPermissions();
        List<SettingsSectionResponse> sections = new ArrayList<>(List.of(
                new SettingsSectionResponse("conta", "Conta", "Identidade do usuario, plano e contexto do workspace.", "live", "live"),
                new SettingsSectionResponse("configuracoes", "Configuracoes", "Idioma, aparencia e comunicacao da plataforma.", "live", "live"),
                new SettingsSectionResponse("uso", "Uso", "Resumo operacional, creditos e historico de utilizacao.", "live", "live"),
                new SettingsSectionResponse("agendadas", "Tarefas agendadas", "Fila de tarefas futuras e estados de execucao.", "preview", "preview"),
                new SettingsSectionResponse("mail", "Mail", "Entrada por e-mail para disparo e automacao de tarefas.", "preview", "disabled-preview"),
                new SettingsSectionResponse("dados", "Controles de dados", "Gestao de itens compartilhados e trilha operacional.", "preview", "preview"),
                new SettingsSectionResponse("navegador", "Navegador em nuvem", "Perfis, cookies e persistencia operacional futura.", "preview", "disabled-preview"),
                new SettingsSectionResponse("personalizacao", "Personalizacao", "Perfil, bio e instrucoes customizadas por workspace.", "preview", "preview"),
                new SettingsSectionResponse("habilidades", "Habilidades", "Skills oficiais e extensoes customizadas.", "preview", "disabled-preview"),
                new SettingsSectionResponse("conectores", "Conectores", "Conexoes nativas com provedores e sistemas externos.", "preview", "disabled-preview"),
                new SettingsSectionResponse("integracoes", "Integracoes", "APIs, webhooks e automacoes externas.", "preview", "disabled-preview")
        ));
        if (permissions.contains(WorkspaceContextService.PERMISSION_KNOWLEDGE_READ)) {
            sections.add(new SettingsSectionResponse(
                    "knowledge",
                    "Knowledge",
                    "Fontes de conhecimento, contexto permissionado e readiness para agents.",
                    "live",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_BUDGETS_READ)) {
            sections.add(new SettingsSectionResponse(
                    "finops",
                    "FinOps & Budgets",
                    "Budget do workspace, chargeback/showback e guardrails operacionais.",
                    "live",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_PROVIDERS_READ)) {
            sections.add(new SettingsSectionResponse(
                    "providers-runtime",
                    "Providers & Runtime",
                    "Catalogo versionado, credenciais exigidas e runtime real dos agentes.",
                    "live",
                    "live"
            ));
        }
        if (permissions.contains(WorkspaceContextService.PERMISSION_THREAT_INTEL_READ)) {
            sections.add(new SettingsSectionResponse(
                    "threat-intelligence",
                    "Threat Intelligence",
                    "Superficie administrativa, auditada e desligada por padrao para provedores dark web e threat-intel.",
                    "preview",
                    "disabled-preview"
            ));
        }

        return new SettingsOverviewResponse(
                workspaceContextService.getOrganizationName(),
                workspaceContextService.getWorkspaceName(),
                workspaceContextService.getSession().role().label(),
                notificationService.unreadCount(),
                knowledgeSourceService.countSources(),
                usageService.getSummary(),
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
