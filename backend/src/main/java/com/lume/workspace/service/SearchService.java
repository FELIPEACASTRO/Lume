package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.SearchResultResponse;
import com.lume.workspace.dto.SearchResultsResponse;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
import com.lume.workspace.repository.LibraryEntryJpaRepository;
import com.lume.workspace.repository.ProjectJpaRepository;
import com.lume.workspace.repository.PromptTemplateJpaRepository;
import com.lume.workspace.repository.TaskJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final JpaUserRepository userRepository;
    private final LibraryEntryJpaRepository libraryEntryRepository;
    private final AgentProfileJpaRepository agentProfileRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final KnowledgeSourceJpaRepository knowledgeSourceRepository;
    private final ProjectJpaRepository projectRepository;
    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final TaskJpaRepository taskRepository;
    private final WorkspaceContextService workspaceContextService;

    public SearchService(
            JpaUserRepository userRepository,
            LibraryEntryJpaRepository libraryEntryRepository,
            AgentProfileJpaRepository agentProfileRepository,
            AgentThreadJpaRepository agentThreadRepository,
            KnowledgeSourceJpaRepository knowledgeSourceRepository,
            ProjectJpaRepository projectRepository,
            PromptTemplateJpaRepository promptTemplateRepository,
            TaskJpaRepository taskRepository,
            WorkspaceContextService workspaceContextService
    ) {
        this.userRepository = userRepository;
        this.libraryEntryRepository = libraryEntryRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.agentThreadRepository = agentThreadRepository;
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.projectRepository = projectRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.taskRepository = taskRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<SearchResultResponse> search(String query) {
        return searchInternal(query, 24, true);
    }

    public SearchResultsResponse searchResults(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        List<SearchResultResponse> results = searchInternal(normalizedQuery, 60, false);
        return new SearchResultsResponse(normalizedQuery, results.size(), results);
    }

    private List<SearchResultResponse> searchInternal(String query, int limit, boolean includeBlankQueryResults) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isBlank() && !includeBlankQueryResults) {
            return List.of();
        }
        List<SearchResultResponse> results = new ArrayList<>();

        Long workspaceId = workspaceContextService.getWorkspaceId();
        int categoryLimit = categoryLimit(limit);

        for (UserJpaEntity user : loadMembers(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "user-%d".formatted(user.getId()),
                    user.getName(),
                    "Pessoa da equipe . %s".formatted(user.getEmail()),
                    "/users",
                    "Equipe",
                    "live",
                    List.of(user.getName(), user.getEmail(), String.valueOf(user.getId()), "equipe", "usuario", "users")
            ), normalizedQuery);
        }

        for (ProjectJpaEntity project : loadProjects(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "project-%s".formatted(project.getId()),
                    project.getName(),
                    defaultText(project.getSummary(), "Projeto do workspace."),
                    "/projects?project=%s".formatted(project.getId()),
                    "Projetos",
                    project.getAvailability(),
                    List.of(project.getName(), project.getSummary(), project.getOwnerName(), "projeto", "project")
            ), normalizedQuery);
        }

        for (TaskJpaEntity task : loadTasks(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "task-%s".formatted(task.getId()),
                    task.getTitle(),
                    defaultText(task.getSummary(), task.getPrompt()),
                    "/tasks/%s".formatted(task.getId()),
                    "Tarefas",
                    task.getAvailability(),
                    List.of(task.getTitle(), task.getPrompt(), task.getSummary(), task.getTaskType(), "tarefa", "task")
            ), normalizedQuery);
        }

        for (LibraryEntryJpaEntity entry : loadLibraryEntries(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "library-%s".formatted(entry.getId()),
                    entry.getTitle(),
                    "%s . %s".formatted(entry.getCategory(), defaultText(entry.getSummary(), "Arquivo do workspace.")),
                    "/library?entry=%s".formatted(entry.getId()),
                    "Biblioteca",
                    entry.getAvailability(),
                    List.of(entry.getTitle(), entry.getCategory(), entry.getOwnerName(), entry.getSummary(), String.join(" ", entry.getTags()))
            ), normalizedQuery);
        }

        for (KnowledgeSourceJpaEntity source : loadKnowledgeSources(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "knowledge-%s".formatted(source.getId()),
                    source.getTitle(),
                    "%s . %s".formatted(source.getSourceType(), defaultText(source.getNote(), "Fonte conectada ao workspace.")),
                    "/settings?section=knowledge",
                    "Conhecimento",
                    source.getAvailability(),
                    List.of(source.getTitle(), source.getSourceType(), source.getNote(), source.getSourceUri() == null ? "" : source.getSourceUri(), "conhecimento", "fonte")
            ), normalizedQuery);
        }

        for (PromptTemplateJpaEntity template : loadTemplates(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "template-%s".formatted(template.getId()),
                    template.getTitle(),
                    "%s . %s".formatted(template.getTemplateScope(), defaultText(template.getSummary(), "Template operacional do workspace.")),
                    "/agents?template=%s".formatted(template.getId()),
                    "Templates",
                    template.getAvailability(),
                    List.of(template.getTitle(), template.getSummary(), template.getPromptBody(), template.getVariablesRaw(), "template", "prompt")
            ), normalizedQuery);
        }

        for (AgentProfileJpaEntity profile : loadAgentProfiles(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "profile-%s".formatted(profile.getId()),
                    profile.getName(),
                    "%s . %s".formatted(profile.getSpecialty(), defaultText(profile.getNote(), "Configuracao do agente.")),
                    "/agents",
                    "Agentes",
                    profile.getAvailability(),
                    List.of(profile.getName(), profile.getSpecialty(), profile.getDescription(), profile.getNote())
            ), normalizedQuery);
        }

        for (AgentThreadJpaEntity thread : loadAgentThreads(workspaceId, normalizedQuery, categoryLimit)) {
            maybeAdd(results, new SearchResultResponse(
                    "thread-%s".formatted(thread.getId()),
                    thread.getTitle(),
                    thread.getLastMessagePreview() != null ? thread.getLastMessagePreview() : "Thread do agent",
                    "/agents?thread=%s".formatted(thread.getId()),
                    "Agentes",
                    thread.getAvailability(),
                    List.of(thread.getTitle(), thread.getLastMessagePreview() == null ? "" : thread.getLastMessagePreview(), "agent", "thread")
            ), normalizedQuery);
        }

        return results.stream().limit(limit).toList();
    }

    private List<UserJpaEntity> loadMembers(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return userRepository.findActiveMembersByWorkspaceId(workspaceId, page);
        }
        return userRepository.searchActiveMembersByWorkspaceId(workspaceId, query, page);
    }

    private List<ProjectJpaEntity> loadProjects(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return projectRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return projectRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<TaskJpaEntity> loadTasks(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return taskRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return taskRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<LibraryEntryJpaEntity> loadLibraryEntries(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return libraryEntryRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return libraryEntryRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<KnowledgeSourceJpaEntity> loadKnowledgeSources(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return knowledgeSourceRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return knowledgeSourceRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<PromptTemplateJpaEntity> loadTemplates(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return promptTemplateRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return promptTemplateRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<AgentProfileJpaEntity> loadAgentProfiles(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspaceId, page);
        }
        return agentProfileRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private List<AgentThreadJpaEntity> loadAgentThreads(Long workspaceId, String query, int categoryLimit) {
        PageRequest page = PageRequest.of(0, categoryLimit);
        if (query.isBlank()) {
            return agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId, page);
        }
        return agentThreadRepository.searchByWorkspaceId(workspaceId, query, page);
    }

    private int categoryLimit(int requestedLimit) {
        return Math.max(3, Math.min(8, (requestedLimit / 6) + 1));
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

    private String defaultText(String primary, String fallback) {
        if (primary == null || primary.isBlank()) {
            return fallback;
        }
        return primary;
    }
}
