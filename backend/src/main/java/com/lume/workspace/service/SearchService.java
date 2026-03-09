package com.lume.workspace.service;

import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.SearchResultResponse;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.entity.KnowledgeSourceJpaEntity;
import com.lume.workspace.entity.LibraryEntryJpaEntity;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.ProjectJpaEntity;
import com.lume.workspace.entity.PromptTemplateJpaEntity;
import com.lume.workspace.entity.TaskJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.repository.KnowledgeSourceJpaRepository;
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
    private final KnowledgeSourceJpaRepository knowledgeSourceRepository;
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
            KnowledgeSourceJpaRepository knowledgeSourceRepository,
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
        this.knowledgeSourceRepository = knowledgeSourceRepository;
        this.projectRepository = projectRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.taskRepository = taskRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceContextService = workspaceContextService;
    }

    public List<SearchResultResponse> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<SearchResultResponse> results = new ArrayList<>();

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
                    "Membros",
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
                    "Projetos",
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
                    "Tarefas",
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
                    "Biblioteca",
                    entry.getAvailability(),
                    List.of(entry.getTitle(), entry.getCategory(), entry.getOwnerName(), entry.getSummary(), String.join(" ", entry.getTags()))
            ), normalizedQuery);
        }

        for (KnowledgeSourceJpaEntity source : knowledgeSourceRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "knowledge-%s".formatted(source.getId()),
                    source.getTitle(),
                    "%s. %s".formatted(source.getSourceType(), source.getNote()),
                    "/settings?section=knowledge",
                    "Knowledge",
                    source.getAvailability(),
                    List.of(source.getTitle(), source.getSourceType(), source.getNote(), source.getSourceUri() == null ? "" : source.getSourceUri(), "knowledge", "fonte")
            ), normalizedQuery);
        }

        for (PromptTemplateJpaEntity template : promptTemplateRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)) {
            maybeAdd(results, new SearchResultResponse(
                    "template-%s".formatted(template.getId()),
                    template.getTitle(),
                    "%s. %s".formatted(template.getTemplateScope(), template.getSummary()),
                    "/agents?template=%s".formatted(template.getId()),
                    "Templates",
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
                    "Agents",
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
                    "Agents",
                    thread.getAvailability(),
                    List.of(thread.getTitle(), thread.getLastMessagePreview() == null ? "" : thread.getLastMessagePreview(), "agent", "thread")
            ), normalizedQuery);
        }

        return results.stream().limit(24).toList();
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
