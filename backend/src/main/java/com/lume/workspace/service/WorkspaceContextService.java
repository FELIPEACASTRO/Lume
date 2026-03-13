package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.exception.SetupRequiredException;
import com.lume.domain.exception.UnauthorizedException;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.OrganizationResponse;
import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.dto.SessionRoleResponse;
import com.lume.workspace.dto.SessionUserResponse;
import com.lume.workspace.dto.WorkspaceResponse;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.OrganizationJpaEntity;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.OrganizationJpaRepository;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceContextService {

    public static final String HEADER_ACTOR_USER_ID = "X-Lume-Actor-User-Id";

    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_WORKSPACE_READ = WorkspacePermissions.WORKSPACE_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_WORKSPACE_SWITCH = WorkspacePermissions.WORKSPACE_SWITCH;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_MEMBERS_READ = WorkspacePermissions.MEMBERS_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_MEMBERS_MANAGE = WorkspacePermissions.MEMBERS_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_PROVIDERS_READ = WorkspacePermissions.PROVIDERS_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_PROVIDERS_MANAGE = WorkspacePermissions.PROVIDERS_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_PROVIDERS_TEST = WorkspacePermissions.PROVIDERS_TEST;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_AGENTS_RUNTIME_MANAGE = WorkspacePermissions.AGENTS_RUNTIME_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_RESEARCH_RUN = WorkspacePermissions.RESEARCH_RUN;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_KNOWLEDGE_READ = WorkspacePermissions.KNOWLEDGE_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_KNOWLEDGE_MANAGE = WorkspacePermissions.KNOWLEDGE_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_ARTIFACTS_READ = WorkspacePermissions.ARTIFACTS_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_ARTIFACTS_MANAGE = WorkspacePermissions.ARTIFACTS_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_TEMPLATES_READ = WorkspacePermissions.TEMPLATES_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_TEMPLATES_MANAGE = WorkspacePermissions.TEMPLATES_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_BUDGETS_READ = WorkspacePermissions.BUDGETS_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_BUDGETS_MANAGE = WorkspacePermissions.BUDGETS_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_SETTINGS_MANAGE = WorkspacePermissions.SETTINGS_MANAGE;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_THREAT_INTEL_READ = WorkspacePermissions.THREAT_INTEL_READ;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_THREAT_INTEL_RUN = WorkspacePermissions.THREAT_INTEL_RUN;
    /** @deprecated Use {@link WorkspacePermissions} constants directly. */
    @Deprecated public static final String PERMISSION_THREAT_INTEL_MANAGE = WorkspacePermissions.THREAT_INTEL_MANAGE;

    private static final String CONTEXT_ATTR = "lume.resolvedContext";

    private final OrganizationJpaRepository organizationRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final JpaUserRepository userRepository;
    private final MembershipJpaRepository membershipRepository;
    private final RoleJpaRepository roleRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final WorkspaceSessionService workspaceSessionService;
    private final List<WorkspaceActorOverrideResolver> actorOverrideResolvers;
    private final WorkspacePermissionResolver workspacePermissionResolver;

    @Autowired
    public WorkspaceContextService(
            OrganizationJpaRepository organizationRepository,
            WorkspaceJpaRepository workspaceRepository,
            JpaUserRepository userRepository,
            MembershipJpaRepository membershipRepository,
            RoleJpaRepository roleRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            ObjectProvider<HttpServletRequest> requestProvider,
            WorkspaceSessionService workspaceSessionService,
            List<WorkspaceActorOverrideResolver> actorOverrideResolvers,
            WorkspacePermissionResolver workspacePermissionResolver
    ) {
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.requestProvider = requestProvider;
        this.workspaceSessionService = workspaceSessionService;
        this.actorOverrideResolvers = actorOverrideResolvers;
        this.workspacePermissionResolver = workspacePermissionResolver;
    }

    protected WorkspaceContextService(
            OrganizationJpaRepository organizationRepository,
            WorkspaceJpaRepository workspaceRepository,
            JpaUserRepository userRepository,
            MembershipJpaRepository membershipRepository,
            RoleJpaRepository roleRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            ObjectProvider<HttpServletRequest> requestProvider
    ) {
        this(
                organizationRepository,
                workspaceRepository,
                userRepository,
                membershipRepository,
                roleRepository,
                userPreferenceRepository,
                requestProvider,
                null,
                List.of(),
                new WorkspacePermissionResolver()
        );
    }

    public SessionContextResponse getSession() {
        CurrentContext context = resolveContextCached(resolveCurrentUser());
        return toSession(context);
    }

    public SessionContextResponse getSessionForUser(UserJpaEntity actor) {
        return toSession(resolveContextCached(actor));
    }

    private SessionContextResponse toSession(CurrentContext context) {
        return new SessionContextResponse(
                new SessionUserResponse(
                        context.user().getId(),
                        context.user().getName(),
                        context.user().getEmail(),
                        initialsFor(context.user().getName())
                ),
                new OrganizationResponse(
                        context.organization().getId(),
                        context.organization().getName(),
                        context.organization().getSlug()
                ),
                new WorkspaceResponse(
                        context.workspace().getId(),
                        context.workspace().getName(),
                        context.workspace().getSlug()
                ),
                new SessionRoleResponse(
                        context.role().getCode(),
                        context.role().getLabel(),
                        workspacePermissionResolver.permissionsFor(context.role().getCode())
                )
        );
    }

    public Long getOrganizationId() {
        return resolveContextCached(resolveCurrentUser()).organization().getId();
    }

    public Long getWorkspaceId() {
        return resolveContextCached(resolveCurrentUser()).workspace().getId();
    }

    public String getWorkspaceName() {
        return resolveContextCached(resolveCurrentUser()).workspace().getName();
    }

    public String getOrganizationName() {
        return resolveContextCached(resolveCurrentUser()).organization().getName();
    }

    public String getActorName() {
        return resolveCurrentUser().getName();
    }

    public Long getActorUserIdOrNull() {
        return resolveCurrentUser().getId();
    }

    public String getCurrentRoleCode() {
        return resolveContextCached(resolveCurrentUser()).role().getCode();
    }

    public List<String> getCurrentPermissions() {
        return workspacePermissionResolver.permissionsFor(getCurrentRoleCode());
    }

    public void requirePermission(String permission) {
        if (!getCurrentPermissions().contains(permission)) {
            throw new AccessDeniedException("Voce nao possui permissao para executar esta acao neste workspace.");
        }
    }

    public boolean hasAccessToWorkspace(Long workspaceId) {
        UserJpaEntity user = resolveCurrentUser();
        return membershipRepository.findByUserIdAndWorkspaceIdAndActiveTrue(user.getId(), workspaceId).isPresent();
    }

    private CurrentContext resolveContext(UserJpaEntity actor) {
        List<MembershipJpaEntity> memberships = membershipRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(actor.getId());
        if (memberships.isEmpty()) {
            throw new AccessDeniedException("O usuario atual nao possui membership ativa em nenhum workspace.");
        }

        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(actor.getId()).orElse(null);
        Long preferredWorkspaceId = preference != null && preference.getActiveWorkspaceId() != null
                ? preference.getActiveWorkspaceId()
                : actor.getWorkspaceId();

        MembershipJpaEntity membership = memberships.stream()
                .filter(item -> item.getWorkspaceId().equals(preferredWorkspaceId))
                .findFirst()
                .orElse(memberships.get(0));

        WorkspaceJpaEntity workspace = workspaceRepository.findById(membership.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", membership.getWorkspaceId()));
        OrganizationJpaEntity organization = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizacao", membership.getOrganizationId()));
        RoleJpaEntity role = roleRepository.findById(membership.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", membership.getRoleId()));

        return new CurrentContext(actor, organization, workspace, membership, role);
    }

    private UserJpaEntity resolveCurrentUser() {
        if (organizationRepository.count() == 0L && workspaceRepository.count() == 0L && userRepository.count() == 0L) {
            throw new SetupRequiredException("A aplicacao ainda nao concluiu o setup inicial.");
        }

        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request != null) {
            for (WorkspaceActorOverrideResolver actorOverrideResolver : actorOverrideResolvers) {
                var override = actorOverrideResolver.resolveOverride(request);
                if (override.isPresent()) {
                    return override.get();
                }
            }
        }

        if (workspaceSessionService == null) {
            throw new UnauthorizedException("A sessao atual nao esta autenticada.");
        }

        return workspaceSessionService.resolveAuthenticatedUser(request)
                .orElseThrow(() -> new UnauthorizedException("A sessao atual nao esta autenticada."));
    }

    private CurrentContext resolveContextCached(UserJpaEntity actor) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request != null) {
            CurrentContext cached = (CurrentContext) request.getAttribute(CONTEXT_ATTR);
            if (cached != null) return cached;
        }
        CurrentContext context = resolveContext(actor);
        if (request != null) {
            request.setAttribute(CONTEXT_ATTR, context);
        }
        return context;
    }

    private String initialsFor(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    private record CurrentContext(
            UserJpaEntity user,
            OrganizationJpaEntity organization,
            WorkspaceJpaEntity workspace,
            MembershipJpaEntity membership,
            RoleJpaEntity role
    ) {
    }
}
