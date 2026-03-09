package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.ArtifactVersionJpaEntity;
import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.NotificationJpaEntity;
import com.lume.workspace.entity.OrganizationJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.entity.TaskStepJpaEntity;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.ArtifactVersionJpaRepository;
import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.NotificationJpaRepository;
import com.lume.workspace.repository.OrganizationJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.PromptTemplateJpaRepository;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import com.lume.workspace.repository.TaskStepJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "lume.workspace.bootstrap", name = "enabled", havingValue = "true")
public class WorkspaceBootstrapDataInitializer implements ApplicationRunner {

    private final OrganizationJpaRepository organizationRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final AgentProfileJpaRepository agentProfileRepository;
    private final ArtifactVersionJpaRepository artifactVersionRepository;
    private final ProjectJpaRepository projectRepository;
    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final TaskJpaRepository taskRepository;
    private final TaskStepJpaRepository taskStepRepository;
    private final NotificationJpaRepository notificationRepository;
    private final KnowledgeSourceJpaRepository knowledgeSourceRepository;
    private final JpaUserRepository userRepository;
    private final MembershipJpaRepository membershipRepository;
    private final RoleJpaRepository roleRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkspaceBootstrapDataInitializer(
            OrganizationJpaRepository organizationRepository,
            WorkspaceJpaRepository workspaceRepository,
            LibraryEntryJpaRepository libraryEntryRepository,
            AgentProfileJpaRepository agentProfileRepository,
            ArtifactVersionJpaRepository artifactVersionRepository,
            ProjectJpaRepository projectRepository,
            PromptTemplateJpaRepository promptTemplateRepository,
            TaskJpaRepository taskRepository,
            TaskStepJpaRepository taskStepRepository,
            NotificationJpaRepository notificationRepository,
            KnowledgeSourceJpaRepository knowledgeSourceRepository,
            JpaUserRepository userRepository,
            MembershipJpaRepository membershipRepository,
            RoleJpaRepository roleRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.libraryEntryRepository = libraryEntryRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.artifactVersionRepository = artifactVersionRepository;
        this.projectRepository = projectRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.taskRepository = taskRepository;
        this.taskStepRepository = taskStepRepository;
        this.notificationRepository = notificationRepository;
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        OrganizationJpaEntity organization = ensureOrganization();
        WorkspaceJpaEntity primaryWorkspace = ensureWorkspace(organization, "workspace-principal", "Workspace Principal");
        WorkspaceJpaEntity strategyWorkspace = ensureWorkspace(organization, "workspace-strategy", "Workspace Strategy");

        RoleJpaEntity adminRole = ensureRole("workspace_admin", "Workspace Admin", "Acesso administrativo ao workspace");
        RoleJpaEntity memberRole = ensureRole("workspace_member", "Workspace Member", "Acesso operacional ao workspace");

        UserJpaEntity operator = ensureUser("Lume Operator", "operator@lume.local", organization.getId(), primaryWorkspace.getId());
        UserJpaEntity analyst = ensureUser("Ana Strategy", "ana.strategy@lume.local", organization.getId(), strategyWorkspace.getId());

        ensureMembership(operator, organization.getId(), primaryWorkspace.getId(), adminRole.getId());
        ensureMembership(operator, organization.getId(), strategyWorkspace.getId(), adminRole.getId());
        ensureMembership(analyst, organization.getId(), primaryWorkspace.getId(), memberRole.getId());
        ensureMembership(analyst, organization.getId(), strategyWorkspace.getId(), memberRole.getId());

        ensurePreference(operator.getId(), primaryWorkspace.getId());
        ensurePreference(analyst.getId(), strategyWorkspace.getId());

        seedPrimaryWorkspace(primaryWorkspace);
        seedStrategyWorkspace(strategyWorkspace);
    }

    private OrganizationJpaEntity ensureOrganization() {
        return organizationRepository.findBySlug("lume").orElseGet(() -> {
            OrganizationJpaEntity entity = new OrganizationJpaEntity();
            entity.setName("Lume");
            entity.setSlug("lume");
            return organizationRepository.save(entity);
        });
    }

    private WorkspaceJpaEntity ensureWorkspace(OrganizationJpaEntity organization, String slug, String name) {
        return workspaceRepository.findBySlug(slug).orElseGet(() -> {
            WorkspaceJpaEntity workspace = new WorkspaceJpaEntity();
            workspace.setOrganizationId(organization.getId());
            workspace.setName(name);
            workspace.setSlug(slug);
            return workspaceRepository.save(workspace);
        });
    }

    private RoleJpaEntity ensureRole(String code, String label, String description) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            RoleJpaEntity role = new RoleJpaEntity();
            role.setCode(code);
            role.setLabel(label);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private UserJpaEntity ensureUser(String name, String email, Long organizationId, Long workspaceId) {
        return userRepository.findByEmail(email).map(existing -> {
            existing.setName(name);
            existing.setActive(true);
            existing.setOrganizationId(organizationId);
            existing.setWorkspaceId(workspaceId);
            return userRepository.save(existing);
        }).orElseGet(() -> {
            UserJpaEntity user = new UserJpaEntity();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("lume123"));
            user.setActive(true);
            user.setOrganizationId(organizationId);
            user.setWorkspaceId(workspaceId);
            return userRepository.save(user);
        });
    }

    private void ensureMembership(UserJpaEntity user, Long organizationId, Long workspaceId, Long roleId) {
        membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).ifPresentOrElse(existing -> {
            existing.setOrganizationId(organizationId);
            existing.setRoleId(roleId);
            existing.setActive(true);
            membershipRepository.save(existing);
        }, () -> {
            MembershipJpaEntity membership = new MembershipJpaEntity();
            membership.setUserId(user.getId());
            membership.setOrganizationId(organizationId);
            membership.setWorkspaceId(workspaceId);
            membership.setRoleId(roleId);
            membership.setActive(true);
            membershipRepository.save(membership);
        });
    }

    private void ensurePreference(Long userId, Long workspaceId) {
        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(userId).orElseGet(() -> {
            UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
            entity.setUserId(userId);
            return entity;
        });
        if (!"dark".equalsIgnoreCase(preference.getAppearance())) {
            preference.setAppearance("light");
        }
        if (preference.getLanguageCode() == null || preference.getLanguageCode().isBlank()) {
            preference.setLanguageCode("pt-BR");
        }
        if (preference.getActiveWorkspaceId() == null) {
            preference.setActiveWorkspaceId(workspaceId);
        }
        userPreferenceRepository.save(preference);
    }

    private void seedPrimaryWorkspace(WorkspaceJpaEntity workspace) {
        ensureLibraryEntry(workspace.getId(), "lib-onboarding", "Playbook de onboarding", "Playbook", "artifact", "proj-ops", "Operacao", true, "Fluxo mestre para criacao de usuarios, onboarding e comunicacao interna.", "usuarios", "compliance", "ritual");
        ensureLibraryEntry(workspace.getId(), "lib-governanca", "Checklist de governanca", "Checklist", "artifact", "proj-ops", "Financeiro", false, "Lista de validacoes para acessos, politicas internas e evidencias.", "auditoria", "seguranca", "processo");
        ensureLibraryEntry(workspace.getId(), "lib-memoria", "Memoria de reunioes do time", "Memoria", "artifact", "proj-growth", "Produto", false, "Resumos acionaveis de reunioes com decisoes, riscos e owners registrados.", "meeting", "owners", "roadmap");

        ensureAgentProfile(workspace.getId(), "ops", "Ops Strategist", "Operacao e processos", "Traduz pedidos em fluxos executaveis com foco em custo, risco e velocidade.", "openai", "openai:gpt-4.1-mini", "agent-v1-openai", "Voce atua como operador senior. Responda com diagnostico, riscos, trade-offs e proximo passo executavel.");
        ensureAgentProfile(workspace.getId(), "growth", "Growth Architect", "Expansao e ativacao", "Estrutura campanhas, argumentos e alavancas de conversao com contexto do workspace.", "anthropic", "anthropic:claude-sonnet-4-5", "agent-v1-claude", "Voce atua como arquiteto de growth. Estruture a resposta em hipoteses, narrativa, experimento e criterio de sucesso.");
        ensureAgentProfile(workspace.getId(), "compliance", "Compliance Analyst", "Controles e validacoes", "Valida regras, checkpoints e politicas antes de publicar qualquer fluxo.", "google-gemini", "google-gemini:gemini-2.5-pro", "agent-v1-gemini-pro", "Voce atua como analista de compliance. Priorize controle, evidencias, risco residual e linguagem conservadora.");
        ensureAgentProfile(workspace.getId(), "deepseek-research", "DeepSeek Researcher", "Analise e raciocinio", "Cruza contexto e monta respostas enxutas para investigacao e decisao.", "deepseek", "deepseek:deepseek-chat", "agent-v1-deepseek", "Voce atua como pesquisador pragmatico. Priorize clareza analitica, estrutura e proximos passos.");
        ensureAgentProfile(workspace.getId(), "grok-scout", "Grok Scout", "Sinais e exploracao", "Explora hipoteses e devolve leituras objetivas para debate rapido.", "xai", "xai:grok-4", "agent-v1-grok", "Voce atua como scout de sinais. Traga leitura objetiva, comparacoes e pontos de tensao do problema.");
        ensureAgentProfile(workspace.getId(), "sonar-briefing", "Sonar Briefing", "Pesquisa assistida", "Resume contexto com linguagem de briefing e foco em priorizacao.", "perplexity", "perplexity:sonar", "agent-v1-sonar", "Voce atua como sintetizador de briefing. Responda com resumo, contexto e recomendacao acionavel.");

        ensureProject(workspace.getId(), "proj-ops", "Operacao do workspace", "Organiza onboarding, permissoes e checkpoints do workspace atual.", "Operacao", "API real");
        ensureProject(workspace.getId(), "proj-growth", "Growth e ativacao", "Centraliza campanhas, narrativas e alavancas de crescimento monitoradas pelo time.", "Growth", "API real");

        ensureArtifactVersion(workspace.getId(), "ver-lib-onboarding-v1", "lib-onboarding", "v1", "Playbook inicial persistido", "Estrutura base de onboarding, ownership e checkpoints para ativacao.");
        ensureArtifactVersion(workspace.getId(), "ver-lib-onboarding-v2", "lib-onboarding", "v2", "Versionamento do fluxo de aprovacao", "Inclui checkpoints de compliance, handoff para growth e criterio de aceite.");
        ensureArtifactVersion(workspace.getId(), "ver-lib-governanca-v1", "lib-governanca", "v1", "Checklist publicado", "Primeira versao operacional com trilha de auditoria e owners definidos.");
        ensureArtifactVersion(workspace.getId(), "ver-lib-memoria-v1", "lib-memoria", "v1", "Memoria consolidada", "Decisoes, riscos e pendencias registradas para o proximo ciclo.");

        ensureTask(workspace.getId(), "task-onboarding", "proj-ops", "playbook", "Estruturar onboarding operacional do workspace", "Mapeie fluxo de onboarding, checkpoints de aprovacao e owners.", "Playbook persistido para o workspace atual.");
        ensureTaskStep("task-onboarding", 1, "plan", "Definir escopo do onboarding", "Consolidar quais perfis entram, quais aprovacoes sao obrigatorias e como a trilha sera medida.", "completed");
        ensureTaskStep("task-onboarding", 2, "context", "Consultar biblioteca do workspace", "Cruzar playbooks e checklists ja existentes antes de publicar a nova rotina.", "completed");
        ensureTaskStep("task-onboarding", 3, "execution", "Preparar execucao assistida", "A tarefa esta persistida e pronta para seguir para inferencia unificada nas proximas fases.", "running");

        ensureTask(workspace.getId(), "task-site", "proj-growth", "sites", "Organizar site de campanha para o trimestre", "Crie a estrutura de paginas, mensagens e blocos de prova social.", "Tarefa registrada como execucao assistida antes da inferencia real.");
        ensureTaskStep("task-site", 1, "plan", "Definir estrutura da campanha", "Criar paginas, mensagens centrais e blocos de captura associados ao projeto.", "completed");
        ensureTaskStep("task-site", 2, "execution", "Planejar deploy e aprovacoes", "O fluxo aguarda integracoes reais de site generation e publish.", "running");

        ensureTask(workspace.getId(), "task-research", null, "research", "Levantar riscos de compliance antes do rollout", "Analise riscos, evidencias e requisitos para publicar a nova automacao.", "Pesquisa persistida e pronta para compartilhamento futuro.");
        ensureTaskStep("task-research", 1, "plan", "Listar perguntas criticas", "Consolidar perguntas abertas, risco residual e owners para cada frente.", "completed");
        ensureTaskStep("task-research", 2, "context", "Coletar evidencias do workspace", "Cruzar biblioteca, usuarios e historico do time para montar a analise.", "running");

        ensureNotification(workspace.getId(), "notif-task-onboarding", "task", "Task pronta para revisao", "A tarefa de onboarding ja possui plano inicial, contexto recuperado e proximo passo definido.", "/tasks/task-onboarding", false);
        ensureNotification(workspace.getId(), "notif-library-sync", "library", "Biblioteca sincronizada", "Os artefatos centrais do workspace foram indexados para busca global.", "/library", false);
        ensureNotification(workspace.getId(), "notif-agents", "agent", "Threads de agents persistidas", "O modulo de agents continua em preview, mas as conversas ja estao no backend.", "/agents", true);

        ensureKnowledgeSource(workspace.getId(), "knowledge-playbooks", "Playbooks operacionais", "library", "proj-ops", "lume://library/playbooks", 3, true, "API real", "Conjunto de artefatos persistidos na biblioteca do workspace.");
        ensureKnowledgeSource(workspace.getId(), "knowledge-agents", "Contexto de agents", "agent-thread", null, "lume://agents/threads", 2, true, "Preview assistido", "Threads e mensagens reais aguardando a camada de inferencia unificada.");

        ensurePromptTemplate(workspace.getId(), "tpl-ops-onboarding", "Playbook de onboarding", "Template para abrir fluxos de onboarding com checkpoints e owners.", "Mapeie um onboarding operacional para {{workspace}} com etapas, owners, riscos e criterio de aceite.", "workspace", "proj-ops", "ops", true, "workspace,owners,risco,aceite");
        ensurePromptTemplate(workspace.getId(), "tpl-growth-brief", "Brief de campanha", "Template para organizar tese, mensagem e experimento de growth.", "Monte um brief de campanha para {{objetivo}} com ICP, proposta de valor, prova social e experimento inicial.", "project", "proj-growth", "growth", false, "objetivo,icp,prova_social");
        ensurePromptTemplate(workspace.getId(), "tpl-compliance-review", "Checklist de compliance", "Template para revisao conservadora antes de publicar fluxos.", "Revise o fluxo {{fluxo}} com riscos, evidencias faltantes, aprovacoes necessarias e bloqueios.", "agent", "proj-ops", "compliance", true, "fluxo,evidencias,aprovacoes");
    }

    private void seedStrategyWorkspace(WorkspaceJpaEntity workspace) {
        ensureLibraryEntry(workspace.getId(), "lib-strategy-brief", "Brief de estrategia", "Brief", "artifact", "proj-strategy", "Estrategia", true, "Resumo tatico do workspace voltado a pricing, posicionamento e canais.", "pricing", "positioning", "gtm");
        ensureLibraryEntry(workspace.getId(), "lib-strategy-research", "Radar competitivo", "Research", "artifact", "proj-strategy", "Produto", false, "Coleta de sinais do mercado e hipoteses de diferenciacao da Lume.", "benchmark", "research", "signals");

        ensureAgentProfile(workspace.getId(), "strategy", "Strategy Operator", "Planejamento e portfolio", "Traduz benchmark, pricing e portfolio em trilhas acionaveis.", "openai", "openai:gpt-4.1-mini", "agent-v1-openai", "Voce atua como operador de estrategia. Estruture resposta em tese, evidencias, riscos e decisao recomendada.");

        ensureProject(workspace.getId(), "proj-strategy", "Planejamento go-to-market", "Coordena posicionamento, assets e prioridades do proximo ciclo.", "Estrategia", "API real");

        ensureArtifactVersion(workspace.getId(), "ver-lib-strategy-brief-v1", "lib-strategy-brief", "v1", "Brief inicial", "Primeiro consolidado de tese, ICP e narrativa central.");
        ensureArtifactVersion(workspace.getId(), "ver-lib-strategy-research-v1", "lib-strategy-research", "v1", "Radar competitivo base", "Sinais de benchmark, pricing e diferenciacao priorizados.");

        ensureTask(workspace.getId(), "task-strategy", "proj-strategy", "research", "Consolidar roteiro de posicionamento", "Cruze benchmark, pricing e ICP para definir o proximo pacote de mensagens.", "Analise de posicionamento persistida neste workspace.");
        ensureTaskStep("task-strategy", 1, "plan", "Definir tese central", "Unificar proposta de valor, ICP e principais objecoes do mercado.", "completed");
        ensureTaskStep("task-strategy", 2, "context", "Coletar sinais competitivos", "Cruzar ativos internos com o benchmark funcional e comercial.", "running");

        ensureNotification(workspace.getId(), "notif-strategy", "strategy", "Workspace de estrategia ativo", "Este workspace possui backlog e contexto proprio para validacao de mercado.", "/projects", false);

        ensureKnowledgeSource(workspace.getId(), "knowledge-strategy", "Fontes de estrategia", "library", "proj-strategy", "lume://library/strategy", 2, true, "API real", "Briefs e evidencias organizadas para experimentos de posicionamento.");

        ensurePromptTemplate(workspace.getId(), "tpl-strategy-thesis", "Tese de posicionamento", "Template para consolidar tese, risco e decisao recomendada.", "Estruture a tese para {{segmento}} com benchmark, diferenciacao, risco e proxima decisao.", "project", "proj-strategy", "strategy", true, "segmento,benchmark,diferenciacao");
    }

    private void ensureLibraryEntry(
            Long workspaceId,
            String id,
            String title,
            String category,
            String entryType,
            String projectId,
            String ownerName,
            boolean favorited,
            String summary,
            String... tags
    ) {
        LibraryEntryJpaEntity entry = libraryEntryRepository.findById(id).orElseGet(LibraryEntryJpaEntity::new);
        entry.setId(id);
        entry.setWorkspaceId(workspaceId);
        entry.setTitle(title);
        entry.setCategory(category);
        entry.setEntryType(entryType);
        entry.setProjectId(projectId);
        entry.setStatusLabel("API real");
        entry.setAvailability("live");
        entry.setOwnerName(ownerName);
        entry.setSourceLabel("Backend do workspace");
        entry.setFavorited(favorited);
        entry.setArchived(false);
        entry.setSummary(summary);
        entry.setTags(new LinkedHashSet<>(List.of(tags)));
        libraryEntryRepository.save(entry);
    }

    private void ensureAgentProfile(
            Long workspaceId,
            String id,
            String name,
            String specialty,
            String description,
            String providerCode,
            String modelCode,
            String versionLabel,
            String systemPrompt
    ) {
        AgentProfileJpaEntity profile = agentProfileRepository.findById(id).orElseGet(AgentProfileJpaEntity::new);
        profile.setId(id);
        profile.setWorkspaceId(workspaceId);
        profile.setName(name);
        profile.setSpecialty(specialty);
        profile.setDescription(description);
        profile.setStatusLabel("Agente versionado");
        profile.setAvailability("preview");
        profile.setNote("O perfil ja possui provider, modelo e versao definidos. A execucao real depende apenas da credencial do provedor.");
        profile.setProviderCode(providerCode);
        profile.setModelCode(modelCode);
        profile.setVersionLabel(versionLabel);
        profile.setSystemPrompt(systemPrompt);
        agentProfileRepository.save(profile);
    }

    private void ensureProject(Long workspaceId, String id, String name, String summary, String ownerName, String statusLabel) {
        if (projectRepository.existsById(id)) {
            return;
        }
        ProjectJpaEntity project = new ProjectJpaEntity();
        project.setId(id);
        project.setWorkspaceId(workspaceId);
        project.setName(name);
        project.setSummary(summary);
        project.setOwnerName(ownerName);
        project.setStatusLabel(statusLabel);
        project.setAvailability("live");
        projectRepository.save(project);
    }

    private void ensureTask(Long workspaceId, String id, String projectId, String taskType, String title, String prompt, String summary) {
        if (taskRepository.existsById(id)) {
            return;
        }
        TaskJpaEntity task = new TaskJpaEntity();
        task.setId(id);
        task.setWorkspaceId(workspaceId);
        task.setProjectId(projectId);
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setPrompt(prompt);
        task.setSummary(summary);
        task.setStatusLabel("Registrada");
        task.setAvailability("live");
        task.setRuntimeState("queued");
        task.setLastError(null);
        task.setOwnerName("Operacao");
        task.setScheduledFor(null);
        task.setShareSlug("shared-" + id);
        taskRepository.save(task);
    }

    private void ensureTaskStep(String taskId, int stepOrder, String stepType, String title, String detail, String statusLabel) {
        if (!taskStepRepository.findByTaskIdOrderByStepOrderAsc(taskId).stream().filter(step -> step.getStepOrder() == stepOrder).toList().isEmpty()) {
            return;
        }
        TaskStepJpaEntity step = new TaskStepJpaEntity();
        step.setId("seed-step-" + UUID.randomUUID().toString().substring(0, 8));
        step.setTaskId(taskId);
        step.setStepOrder(stepOrder);
        step.setStepType(stepType);
        step.setTitle(title);
        step.setDetail(detail);
        step.setStatusLabel(statusLabel);
        taskStepRepository.save(step);
    }

    private void ensureNotification(Long workspaceId, String id, String kind, String title, String body, String path, boolean read) {
        if (notificationRepository.existsById(id)) {
            return;
        }
        NotificationJpaEntity notification = new NotificationJpaEntity();
        notification.setId(id);
        notification.setWorkspaceId(workspaceId);
        notification.setKind(kind);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setPath(path);
        notification.setRead(read);
        notificationRepository.save(notification);
    }

    private void ensureKnowledgeSource(
            Long workspaceId,
            String id,
            String title,
            String sourceType,
            String projectId,
            String sourceUri,
            int documentCount,
            boolean enabledForAgents,
            String statusLabel,
            String note
    ) {
        if (knowledgeSourceRepository.existsById(id)) {
            return;
        }
        KnowledgeSourceJpaEntity source = new KnowledgeSourceJpaEntity();
        source.setId(id);
        source.setWorkspaceId(workspaceId);
        source.setTitle(title);
        source.setSourceType(sourceType);
        source.setProjectId(projectId);
        source.setSourceUri(sourceUri);
        source.setDocumentCount(documentCount);
        source.setEnabledForAgents(enabledForAgents);
        source.setStatusLabel(statusLabel);
        source.setAvailability("live");
        source.setNote(note);
        source.setLastIndexedAt(java.time.LocalDateTime.now());
        knowledgeSourceRepository.save(source);
    }

    private void ensureArtifactVersion(
            Long workspaceId,
            String id,
            String entryId,
            String versionLabel,
            String changeSummary,
            String contentPreview
    ) {
        ArtifactVersionJpaEntity version = artifactVersionRepository.findById(id).orElseGet(ArtifactVersionJpaEntity::new);
        version.setId(id);
        version.setEntryId(entryId);
        version.setWorkspaceId(workspaceId);
        version.setVersionLabel(versionLabel);
        version.setChangeSummary(changeSummary);
        version.setContentPreview(contentPreview);
        version.setCreatedByName("Lume Operator");
        artifactVersionRepository.save(version);
    }

    private void ensurePromptTemplate(
            Long workspaceId,
            String id,
            String title,
            String summary,
            String promptBody,
            String templateScope,
            String projectId,
            String agentProfileId,
            boolean favorited,
            String variablesRaw
    ) {
        PromptTemplateJpaEntity template = promptTemplateRepository.findById(id).orElseGet(PromptTemplateJpaEntity::new);
        template.setId(id);
        template.setWorkspaceId(workspaceId);
        template.setProjectId(projectId);
        template.setAgentProfileId(agentProfileId);
        template.setTitle(title);
        template.setSummary(summary);
        template.setPromptBody(promptBody);
        template.setTemplateScope(templateScope);
        template.setStatusLabel("Template operacional");
        template.setAvailability("live");
        template.setOwnerName("Lume Operator");
        template.setVariablesRaw(variablesRaw);
        template.setFavorited(favorited);
        promptTemplateRepository.save(template);
    }
}
