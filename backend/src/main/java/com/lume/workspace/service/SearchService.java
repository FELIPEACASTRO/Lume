package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.SearchResultResponse;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.PromptTemplateJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final JpaUserRepository userRepository;
    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final AgentProfileJpaRepository agentProfileRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final ProjectJpaRepository projectRepository;
    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final TaskJpaRepository taskRepository;
    private final MembershipJpaRepository membershipRepository;
    private final WorkspaceContextService workspaceContextService;

    public SearchService(
            JpaUserRepository userRepository,
            LibraryEntryJpaRepository libraryEntryRepository,
            AgentProfileJpaRepository agentProfileRepository,
            AgentThreadJpaRepository agentThreadRepository,
            ProjectJpaRepository projectRepository,
            PromptTemplateJpaRepository promptTemplateRepository,
            TaskJpaRepository taskRepository,
            MembershipJpaRepository membershipRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.userRepository = userRepository;
        this.libraryEntryRepository = libraryEntryRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.agentThreadRepository = agentThreadRepository;
        this.projectRepository = projectRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.taskRepository = taskRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<SearchResultResponse> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<SearchResultResponse> results = new ArrayList<>();

        for (SearchResultResponse route : buildRouteResults()) {
            maybeAdd(results, route, normalizedQuery);
        }

        Long workspaceId = workspaceContextService.getWorkspaceId();

        List<Long> memberIds = membershipRepository.findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc(workspaceId)
                .stream()
                .map(MembershipJpaEntity::getUserId)
                .distinct()
                .toList();

        for (UserJpaEntity user : userRepository.findByIdInOrderByNameAsc(memberIds)) {
            maybeAdd(results, new SearchResultResponse(
                    "user-%d".formatted(user.getId()),
                    user.getName(),
                    "Usuario do workspace: %s".formatted(user.getEmail()),
                    "/users",
                    "Areas reais",
                    "live",
                    List.of(user.getName(), user.getEmail(), String.valueOf(user.getId()), "usuario", "users")
            ), normalizedQuery);
        }

        for (ProjectJpaEntity project : projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "project-%s".formatted(project.getId()),
                    project.getName(),
                    project.getSummary(),
                    "/projects?project=%s".formatted(project.getId()),
                    "Areas reais",
                    project.getAvailability(),
                    List.of(project.getName(), project.getSummary(), project.getOwnerName(), "projeto", "project")
            ), normalizedQuery);
        }

        for (TaskJpaEntity task : taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "task-%s".formatted(task.getId()),
                    task.getTitle(),
                    "%s. %s".formatted(task.getTaskType(), task.getSummary()),
                    "/tasks/%s".formatted(task.getId()),
                    "Atalhos",
                    task.getAvailability(),
                    List.of(task.getTitle(), task.getPrompt(), task.getSummary(), task.getTaskType(), "tarefa", "task")
            ), normalizedQuery);
        }

        for (LibraryEntryJpaEntity entry : libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "library-%s".formatted(entry.getId()),
                    entry.getTitle(),
                    "%s. %s".formatted(entry.getCategory(), entry.getSummary()),
                    "/library?entry=%s".formatted(entry.getId()),
                    "Contexto",
                    entry.getAvailability(),
                    List.of(entry.getTitle(), entry.getCategory(), entry.getOwnerName(), entry.getSummary(), String.join(" ", entry.getTags()))
            ), normalizedQuery);
        }

        for (PromptTemplateJpaEntity template : promptTemplateRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "template-%s".formatted(template.getId()),
                    template.getTitle(),
                    "%s. %s".formatted(template.getTemplateScope(), template.getSummary()),
                    "/agents?template=%s".formatted(template.getId()),
                    "Ativos inteligentes",
                    template.getAvailability(),
                    List.of(template.getTitle(), template.getSummary(), template.getPromptBody(), template.getVariablesRaw(), "template", "prompt")
            ), normalizedQuery);
        }

        for (AgentProfileJpaEntity profile : agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "profile-%s".formatted(profile.getId()),
                    profile.getName(),
                    "%s. %s".formatted(profile.getSpecialty(), profile.getNote()),
                    "/agents",
                    "Areas preview",
                    profile.getAvailability(),
                    List.of(profile.getName(), profile.getSpecialty(), profile.getDescription(), profile.getNote())
            ), normalizedQuery);
        }

        for (AgentThreadJpaEntity thread : agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "thread-%s".formatted(thread.getId()),
                    thread.getTitle(),
                    thread.getLastMessagePreview() != null ? thread.getLastMessagePreview() : "Thread do agent",
                    "/agents?thread=%s".formatted(thread.getId()),
                    "Areas preview",
                    thread.getAvailability(),
                    List.of(thread.getTitle(), thread.getLastMessagePreview() == null ? "" : thread.getLastMessagePreview(), "agent", "thread")
            ), normalizedQuery);
        }

        return results.stream().limit(24).toList();
    }

    private List<SearchResultResponse> buildRouteResults() {
        return List.of(
                new SearchResultResponse(
                        "route-home",
                        "Workspace",
                        "Hub de trabalho, busca global e taxonomia do produto atual.",
                        "/",
                        "Workspace",
                        "live",
                        List.of("workspace", "hub", "inicio", "busca")
                ),
                new SearchResultResponse(
                        "route-users",
                        "Usuarios",
                        "CRUD real conectado ao backend do Lume.",
                        "/users",
                        "Areas reais",
                        "live",
                        List.of("usuarios", "cadastro", "api", "operacao")
                ),
                new SearchResultResponse(
                        "route-projects",
                        "Projetos",
                        "Estrutura ownership, backlog e contexto organizacional do workspace.",
                        "/projects",
                        "Areas reais",
                        "live",
                        List.of("projetos", "projects", "ownership", "backlog")
                ),
                new SearchResultResponse(
                        "route-tasks",
                        "Tarefas",
                        "Task view persistida com steps, follow-ups e status assistido.",
                        "/tasks",
                        "Atalhos",
                        "preview",
                        List.of("tarefas", "tasks", "task view", "execucao")
                ),
                new SearchResultResponse(
                        "route-agents",
                        "Agents",
                        "Threads reais com respostas assistidas enquanto a inferencia ainda esta em preview.",
                        "/agents",
                        "Areas preview",
                        "preview",
                        List.of("agents", "threads", "preview", "prompts")
                ),
                new SearchResultResponse(
                        "route-library",
                        "Biblioteca",
                        "Documentos reais do workspace para contexto e consulta.",
                        "/library",
                        "Contexto",
                        "live",
                        List.of("biblioteca", "contexto", "documentos", "playbooks")
                ),
                new SearchResultResponse(
                        "route-settings",
                        "Settings",
                        "Conta, configuracoes, uso, conectores e personalizacao do workspace.",
                        "/settings",
                        "Atalhos",
                        "preview",
                        List.of("settings", "configuracoes", "conta", "uso", "conectores")
                ),
                new SearchResultResponse(
                        "route-usage",
                        "Uso",
                        "Resumo operacional de creditos, tarefas ativas e notificacoes abertas.",
                        "/usage",
                        "Atalhos",
                        "live",
                        List.of("uso", "usage", "creditos", "budget")
                ),
                new SearchResultResponse(
                        "route-inbox",
                        "Inbox",
                        "Notificacoes operacionais e eventos recentes do workspace.",
                        "/inbox",
                        "Atalhos",
                        "live",
                        List.of("inbox", "notificacoes", "alerts", "activity")
                )
        );
    }

    private void maybeAdd(List<SearchResultResponse> results, SearchResultResponse result, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            results.add(result);
            return;
        }

        String haystack = (result.title() + " " + result.description() + " " + String.join(" ", result.keywords()))
                .toLowerCase();
        if (haystack.contains(normalizedQuery)) {
            results.add(result);
        }
    }
}
