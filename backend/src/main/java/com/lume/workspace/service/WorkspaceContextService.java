package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.ResourceNotFoundException;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkspaceContextService {

    public static final String HEADER_ACTOR_USER_ID = "X-Lume-Actor-User-Id";
    public static final String PERMISSION_WORKSPACE_READ = "workspace.read";
    public static final String PERMISSION_WORKSPACE_SWITCH = "workspace.switch";
    public static final String PERMISSION_MEMBERS_READ = "members.read";
    public static final String PERMISSION_MEMBERS_MANAGE = "members.manage";
    public static final String PERMISSION_PROVIDERS_READ = "providers.read";
    public static final String PERMISSION_PROVIDERS_MANAGE = "providers.manage";
    public static final String PERMISSION_PROVIDERS_TEST = "providers.test";
    public static final String PERMISSION_AGENTS_RUNTIME_MANAGE = "agents.runtime.manage";
    public static final String PERMISSION_RESEARCH_RUN = "research.run";
    public static final String PERMISSION_THREAT_INTEL_READ = "threat_intel.read";
    public static final String PERMISSION_THREAT_INTEL_RUN = "threat_intel.run";
    public static final String PERMISSION_THREAT_INTEL_MANAGE = "threat_intel.manage";

    private final OrganizationJpaRepository organizationRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final JpaUserRepository userRepository;
    private final MembershipJpaRepository membershipRepository;
    private final RoleJpaRepository roleRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public WorkspaceContextService(
            OrganizationJpaRepository organizationRepository,
            WorkspaceJpaRepository workspaceRepository,
            JpaUserRepository userRepository,
            MembershipJpaRepository membershipRepository,
            RoleJpaRepository roleRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            ObjectProvider<HttpServletRequest> requestProvider
    ) {
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.requestProvider = requestProvider;
    }

    public SessionContextResponse getSession() {
        CurrentContext context = resolveContext();
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
                        permissionsFor(context.role().getCode())
                )
        );
    }

    public Long getOrganizationId() {
        return resolveContext().organization().getId();
    }

    public Long getWorkspaceId() {
        return resolveContext().workspace().getId();
    }

    public String getWorkspaceName() {
        return resolveContext().workspace().getName();
    }

    public String getOrganizationName() {
        return resolveContext().organization().getName();
    }

    public Long getActorUserIdOrNull() {
        return resolveCurrentUser().getId();
    }

    public String getCurrentRoleCode() {
        return resolveContext().role().getCode();
    }

    public List<String> getCurrentPermissions() {
        return permissionsFor(getCurrentRoleCode());
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

    private CurrentContext resolveContext() {
        UserJpaEntity actor = resolveCurrentUser();
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
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request != null) {
            String actorUserId = request.getHeader(HEADER_ACTOR_USER_ID);
            if (actorUserId != null && !actorUserId.isBlank()) {
                try {
                    Long parsedId = Long.valueOf(actorUserId.trim());
                    return userRepository.findByIdAndActiveTrue(parsedId)
                            .orElseThrow(() -> new AccessDeniedException("O usuario informado no header nao esta ativo."));
                } catch (NumberFormatException exception) {
                    throw new AccessDeniedException("O header de usuario atual e invalido.");
                }
            }
        }

        return userRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new AccessDeniedException("Nenhum usuario ativo foi encontrado para resolver a sessao atual."));
    }

    private List<String> permissionsFor(String roleCode) {
        return switch (roleCode) {
            case "workspace_admin" -> List.of(
                    PERMISSION_WORKSPACE_READ,
                    PERMISSION_WORKSPACE_SWITCH,
                    PERMISSION_MEMBERS_READ,
                    PERMISSION_MEMBERS_MANAGE,
                    PERMISSION_PROVIDERS_READ,
                    PERMISSION_PROVIDERS_MANAGE,
                    PERMISSION_PROVIDERS_TEST,
                    PERMISSION_AGENTS_RUNTIME_MANAGE,
                    PERMISSION_RESEARCH_RUN,
                    PERMISSION_THREAT_INTEL_READ,
                    PERMISSION_THREAT_INTEL_RUN,
                    PERMISSION_THREAT_INTEL_MANAGE
            );
            case "workspace_member" -> List.of(
                    PERMISSION_WORKSPACE_READ,
                    PERMISSION_WORKSPACE_SWITCH,
                    PERMISSION_RESEARCH_RUN
            );
            default -> List.of(PERMISSION_WORKSPACE_READ);
        };
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
