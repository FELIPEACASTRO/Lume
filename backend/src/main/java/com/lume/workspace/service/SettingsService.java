package com.lume.workspace.service;

import com.lume.workspace.dto.AuditFeedEntryResponse;
import com.lume.workspace.dto.CredentialFieldResponse;
import com.lume.workspace.dto.SettingsComplianceSummaryResponse;
import com.lume.workspace.dto.SettingsOverviewResponse;
import com.lume.workspace.dto.SettingsPreferencesResponse;
import com.lume.workspace.dto.SettingsSectionResponse;
import com.lume.workspace.dto.SettingsUiByokProviderOptionResponse;
import com.lume.workspace.dto.SettingsUiCreditPackOptionResponse;
import com.lume.workspace.dto.SettingsUiOptionItemResponse;
import com.lume.workspace.dto.SettingsUiOptionsResponse;
import com.lume.workspace.dto.UpdateSettingsComplianceRequest;
import com.lume.workspace.dto.UpdateSettingsPreferencesRequest;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class SettingsService {

    private final WorkspaceContextService workspaceContextService;
    private final NotificationService notificationService;
    private final UsageService usageService;
    private final KnowledgeSourceService knowledgeSourceService;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final RoleJpaRepository roleRepository;
    private final AuditLogService auditLogService;
    private final WorkspaceCommercialService workspaceCommercialService;
    private final ComplianceSettingsService complianceSettingsService;
    private final GovernanceSummaryService governanceSummaryService;
    private final ProviderCatalogService providerCatalogService;

    public SettingsService(
            WorkspaceContextService workspaceContextService,
            NotificationService notificationService,
            UsageService usageService,
            KnowledgeSourceService knowledgeSourceService,
            UserPreferenceJpaRepository userPreferenceRepository,
            RoleJpaRepository roleRepository,
            AuditLogService auditLogService,
            WorkspaceCommercialService workspaceCommercialService,
            ComplianceSettingsService complianceSettingsService,
            GovernanceSummaryService governanceSummaryService,
            ProviderCatalogService providerCatalogService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.notificationService = notificationService;
        this.usageService = usageService;
        this.knowledgeSourceService = knowledgeSourceService;
        this.userPreferenceRepository = userPreferenceRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.workspaceCommercialService = workspaceCommercialService;
        this.complianceSettingsService = complianceSettingsService;
        this.governanceSummaryService = governanceSummaryService;
        this.providerCatalogService = providerCatalogService;
    }

    public SettingsOverviewResponse getOverview() {
        Long workspaceId = workspaceContextService.getWorkspaceId();
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
                governanceSummaryService.governanceSummary(workspaceId),
                complianceSettingsService.complianceSummary(),
                sections
        );
    }

    public SettingsPreferencesResponse getPreferences() {
        return toPreferencesResponse(getOrCreatePreferences());
    }

    public SettingsComplianceSummaryResponse getCompliance() {
        return complianceSettingsService.getCompliance();
    }

    public SettingsUiOptionsResponse getUiOptions() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        List<SettingsUiOptionItemResponse> onboardingPrimaryUseCases = List.of(
                option("operations", "Operacoes", "Fluxos operacionais e produtividade do workspace.", true),
                option("research", "Pesquisa", "Investigacao e descoberta orientada por IA.", false),
                option("support", "Suporte", "Atendimento e acompanhamento de incidentes.", false),
                option("content", "Conteudo", "Criacao e iteracao de materiais.", false),
                option("analysis", "Analise", "Analise de dados, documentos e decisao.", false)
        );
        List<SettingsUiOptionItemResponse> onboardingWorkStyles = List.of(
                option("solo_operator", "Operador individual", "Uso individual com foco em velocidade.", false),
                option("small_team", "Time pequeno", "Equipe reduzida com colaboracao operacional.", true),
                option("department_team", "Time departamental", "Operacao por area com governanca local.", false),
                option("multi_team", "Multiplos times", "Escopo multi-time com coordenacao por workspace.", false)
        );
        List<SettingsUiOptionItemResponse> memberRoles = roleRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleJpaEntity::getLabel, String.CASE_INSENSITIVE_ORDER))
                .map(role -> option(
                        role.getCode(),
                        role.getLabel(),
                        role.getDescription(),
                        "workspace_member".equalsIgnoreCase(role.getCode())
                ))
                .toList();
        List<SettingsUiOptionItemResponse> supportCategories = List.of(
                option("operational", "Operacional", "Questoes de fluxo e operacao do workspace.", true),
                option("billing", "Billing", "Cobranca, pagamento e conciliacao.", false),
                option("provider", "Provider", "Integracoes e disponibilidade de providers.", false),
                option("security", "Seguranca", "Eventos ou duvidas de seguranca/compliance.", false),
                option("bug", "Bug", "Falha funcional no produto.", false),
                option("feature_request", "Feature request", "Solicitacao de melhoria funcional.", false)
        );
        List<SettingsUiOptionItemResponse> supportSeverities = List.of(
                option("low", "Low", "Impacto baixo.", false),
                option("medium", "Medium", "Impacto moderado na operacao.", true),
                option("high", "High", "Impacto alto e recorrente.", false),
                option("critical", "Critical", "Impacto critico ou indisponibilidade.", false)
        );
        List<SettingsUiOptionItemResponse> supportStatuses = List.of(
                option("open", "Open", "Ticket aberto aguardando triagem.", true),
                option("in_progress", "In progress", "Ticket em tratamento.", false),
                option("resolved", "Resolved", "Ticket resolvido e validado.", false),
                option("closed", "Closed", "Ticket encerrado.", false)
        );
        List<SettingsUiOptionItemResponse> knowledgeSourceTypes = List.of(
                option("document", "Documento", "Fonte documental para indexacao.", true),
                option("url", "URL", "Conteudo proveniente de URL.", false),
                option("repo", "Repositorio", "Conteudo tecnico em repositorio.", false)
        );
        List<SettingsUiCreditPackOptionResponse> billingCreditPacks = List.of(
                new SettingsUiCreditPackOptionResponse(
                        "pack-starter-500",
                        500,
                        BigDecimal.valueOf(49).setScale(2),
                        "Pack adicional de 500 creditos.",
                        false
                ),
                new SettingsUiCreditPackOptionResponse(
                        "pack-core-1000",
                        1000,
                        BigDecimal.valueOf(99).setScale(2),
                        "Pack adicional de 1.000 creditos.",
                        true
                ),
                new SettingsUiCreditPackOptionResponse(
                        "pack-pro-5000",
                        5000,
                        BigDecimal.valueOf(399).setScale(2),
                        "Pack adicional de 5.000 creditos.",
                        false
                )
        );
        List<SettingsUiByokProviderOptionResponse> byokProviders = providerCatalogService.listProviders().stream()
                .filter(provider -> provider.executionSupported())
                .map(provider -> new SettingsUiByokProviderOptionResponse(
                        provider.code(),
                        provider.name(),
                        resolveSecretRefSuggestion(provider.code(), provider.credentialFields())
                ))
                .toList();
        List<SettingsUiOptionItemResponse> byokScopeOptions = List.of(
                option("workspace", "Workspace", "Escopo restrito ao workspace atual.", true),
                option("organization", "Organizacao", "Escopo compartilhado na organizacao.", false)
        );
        List<SettingsUiOptionItemResponse> complianceRetentionPolicyStatuses = List.of(
                option("not_configured", "Nao configurada", "Politica de retencao ainda nao definida.", true),
                option("configured", "Configurada", "Politica de retencao ativa no workspace.", false)
        );
        List<SettingsUiOptionItemResponse> complianceAccessReviewStatuses = List.of(
                option("not_configured", "Nao configurado", "Ciclo de access review ainda nao definido.", true),
                option("configured", "Configurado", "Ciclo de access review ativo no workspace.", false)
        );

        return new SettingsUiOptionsResponse(
                onboardingPrimaryUseCases,
                onboardingWorkStyles,
                memberRoles,
                supportCategories,
                supportSeverities,
                supportStatuses,
                knowledgeSourceTypes,
                billingCreditPacks,
                byokProviders,
                byokScopeOptions,
                complianceRetentionPolicyStatuses,
                complianceAccessReviewStatuses
        );
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

    public SettingsComplianceSummaryResponse updateCompliance(UpdateSettingsComplianceRequest request) {
        return complianceSettingsService.updateCompliance(request);
    }

    public List<AuditFeedEntryResponse> getAuditFeed(int limit) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_SETTINGS_MANAGE);
        return auditLogService.listCurrentWorkspace(limit);
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

    private SettingsUiOptionItemResponse option(String code, String label, String description, boolean defaultOption) {
        return new SettingsUiOptionItemResponse(code, label, description, defaultOption);
    }

    private String resolveSecretRefSuggestion(String providerCode, List<CredentialFieldResponse> credentialFields) {
        String fromCredentialField = credentialFields.stream()
                .map(CredentialFieldResponse::envVar)
                .filter(envVar -> envVar != null && !envVar.isBlank())
                .findFirst()
                .orElse(null);
        if (fromCredentialField != null) {
            return fromCredentialField;
        }
        return providerCode.toUpperCase(Locale.ROOT).replace('-', '_') + "_API_KEY";
    }

}
