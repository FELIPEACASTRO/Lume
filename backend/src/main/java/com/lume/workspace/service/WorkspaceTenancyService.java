package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.WorkspaceOptionResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceTenancyService {

    private final WorkspaceContextService workspaceContextService;
    private final JpaUserRepository userRepository;
    private final MembershipJpaRepository membershipRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final OrganizationJpaRepository organizationRepository;
    private final RoleJpaRepository roleRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final AuditLogService auditLogService;

    public WorkspaceTenancyService(
            WorkspaceContextService workspaceContextService,
            JpaUserRepository userRepository,
            MembershipJpaRepository membershipRepository,
            WorkspaceJpaRepository workspaceRepository,
            OrganizationJpaRepository organizationRepository,
            RoleJpaRepository roleRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            AuditLogService auditLogService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceRepository = workspaceRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.auditLogService = auditLogService;
    }

    public List<WorkspaceOptionResponse> listAvailableWorkspaces() {
        Long currentUserId = workspaceContextService.getActorUserIdOrNull();
        Long activeWorkspaceId = workspaceContextService.getWorkspaceId();

        return membershipRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(currentUserId).stream()
                .map(membership -> toWorkspaceOption(membership, activeWorkspaceId))
                .sorted(Comparator.comparing(WorkspaceOptionResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public void activateWorkspace(Long workspaceId) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_SWITCH);

        Long actorUserId = workspaceContextService.getActorUserIdOrNull();
        MembershipJpaEntity membership = membershipRepository.findByUserIdAndWorkspaceIdAndActiveTrue(actorUserId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMembership", workspaceId));

        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(actorUserId)
                .orElseGet(() -> {
                    UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
                    entity.setUserId(actorUserId);
                    return entity;
                });
        preference.setActiveWorkspaceId(workspaceId);
        userPreferenceRepository.save(preference);

        UserJpaEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", actorUserId));
        user.setOrganizationId(membership.getOrganizationId());
        user.setWorkspaceId(workspaceId);
        userRepository.save(user);

        auditLogService.record(
                "workspace",
                String.valueOf(workspaceId),
                "activated",
                Map.of("userId", actorUserId)
        );
    }

    private WorkspaceOptionResponse toWorkspaceOption(MembershipJpaEntity membership, Long activeWorkspaceId) {
        WorkspaceJpaEntity workspace = workspaceRepository.findById(membership.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", membership.getWorkspaceId()));
        OrganizationJpaEntity organization = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizacao", membership.getOrganizationId()));
        RoleJpaEntity role = roleRepository.findById(membership.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", membership.getRoleId()));

        return new WorkspaceOptionResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                organization.getId(),
                organization.getName(),
                role.getCode(),
                role.getLabel(),
                workspace.getId().equals(activeWorkspaceId)
        );
    }
}
