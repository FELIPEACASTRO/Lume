package com.lume.workspace.service;

import com.lume.domain.exception.AccessDeniedException;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.MemberResponse;
import com.lume.workspace.dto.UpdateMemberRequest;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.entity.UserPreferenceJpaEntity;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MemberService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final WorkspaceContextService workspaceContextService;
    private final MembershipJpaRepository membershipRepository;
    private final RoleJpaRepository roleRepository;
    private final JpaUserRepository userRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public MemberService(
            WorkspaceContextService workspaceContextService,
            MembershipJpaRepository membershipRepository,
            RoleJpaRepository roleRepository,
            JpaUserRepository userRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.workspaceContextService = workspaceContextService;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<MemberResponse> listMembers() {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_MEMBERS_READ);
        Long workspaceId = workspaceContextService.getWorkspaceId();
        Long actorUserId = workspaceContextService.getActorUserIdOrNull();

        return membershipRepository.findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc(workspaceId).stream()
                .map(membership -> toResponse(membership, actorUserId))
                .toList();
    }

    @Transactional
    public MemberResponse createMember(CreateMemberRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_MEMBERS_MANAGE);

        RoleJpaEntity role = resolveRole(request.roleCode());
        Long workspaceId = workspaceContextService.getWorkspaceId();
        Long organizationId = workspaceContextService.getOrganizationId();

        UserJpaEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .map(existing -> updateExistingUser(existing, request))
                .orElseGet(() -> createUser(request, organizationId, workspaceId));

        membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).ifPresent(existing -> {
            throw new BusinessRuleException("Este usuario ja possui membership neste workspace.");
        });

        MembershipJpaEntity membership = new MembershipJpaEntity();
        membership.setUserId(user.getId());
        membership.setOrganizationId(organizationId);
        membership.setWorkspaceId(workspaceId);
        membership.setRoleId(role.getId());
        membership.setActive(true);
        MembershipJpaEntity savedMembership = membershipRepository.save(membership);

        ensurePreference(user.getId(), workspaceId);

        auditLogService.record(
                "membership",
                String.valueOf(savedMembership.getId()),
                "created",
                Map.of("userId", user.getId(), "roleCode", role.getCode())
        );

        return toResponse(savedMembership, workspaceContextService.getActorUserIdOrNull());
    }

    @Transactional
    public MemberResponse updateMember(Long membershipId, UpdateMemberRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_MEMBERS_MANAGE);

        boolean hasAnyChange = (request.name() != null && !request.name().isBlank())
                || (request.email() != null && !request.email().isBlank())
                || (request.password() != null && !request.password().isBlank())
                || (request.roleCode() != null && !request.roleCode().isBlank())
                || request.active() != null;

        if (!hasAnyChange) {
            throw new IllegalArgumentException("Informe ao menos um campo para atualizacao.");
        }

        MembershipJpaEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));
        if (!membership.getWorkspaceId().equals(workspaceContextService.getWorkspaceId())) {
            throw new AccessDeniedException("Esta membership nao pertence ao workspace ativo.");
        }

        UserJpaEntity user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", membership.getUserId()));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = request.email().trim().toLowerCase();
            userRepository.findByEmail(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("Ja existe um usuario com este email.");
                    });
            user.setEmail(normalizedEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password().trim()));
        }

        if (request.roleCode() != null && !request.roleCode().isBlank()) {
            membership.setRoleId(resolveRole(request.roleCode()).getId());
        }

        if (request.active() != null) {
            Long actorUserId = workspaceContextService.getActorUserIdOrNull();
            if (actorUserId.equals(membership.getUserId()) && !request.active()) {
                throw new BusinessRuleException("Nao e permitido desativar a propria membership ativa.");
            }
            membership.setActive(request.active());
        }

        userRepository.save(user);
        MembershipJpaEntity savedMembership = membershipRepository.save(membership);
        syncUserState(savedMembership);

        auditLogService.record(
                "membership",
                String.valueOf(savedMembership.getId()),
                "updated",
                Map.of(
                        "roleCode", resolveRoleLabel(savedMembership.getRoleId()).getCode(),
                        "active", savedMembership.isActive()
                )
        );

        return toResponse(savedMembership, workspaceContextService.getActorUserIdOrNull());
    }

    private UserJpaEntity createUser(CreateMemberRequest request, Long organizationId, Long workspaceId) {
        UserJpaEntity user = new UserJpaEntity();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password().trim()));
        user.setActive(true);
        user.setOrganizationId(organizationId);
        user.setWorkspaceId(workspaceId);
        return userRepository.save(user);
    }

    private UserJpaEntity updateExistingUser(UserJpaEntity user, CreateMemberRequest request) {
        user.setName(request.name().trim());
        user.setActive(true);
        return userRepository.save(user);
    }

    private void ensurePreference(Long userId, Long workspaceId) {
        UserPreferenceJpaEntity preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreferenceJpaEntity entity = new UserPreferenceJpaEntity();
                    entity.setUserId(userId);
                    return entity;
                });
        if (preference.getActiveWorkspaceId() == null) {
            preference.setActiveWorkspaceId(workspaceId);
        }
        userPreferenceRepository.save(preference);
    }

    private void syncUserState(MembershipJpaEntity membership) {
        UserJpaEntity user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", membership.getUserId()));
        boolean hasActiveMembership = !membershipRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(user.getId()).isEmpty();
        user.setActive(hasActiveMembership);
        userRepository.save(user);

        if (!membership.isActive()) {
            userPreferenceRepository.findByUserId(user.getId()).ifPresent(preference -> {
                if (membership.getWorkspaceId().equals(preference.getActiveWorkspaceId())) {
                    preference.setActiveWorkspaceId(null);
                    userPreferenceRepository.save(preference);
                }
            });
        }
    }

    private MemberResponse toResponse(MembershipJpaEntity membership, Long actorUserId) {
        UserJpaEntity user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", membership.getUserId()));
        RoleJpaEntity role = resolveRoleLabel(membership.getRoleId());

        return new MemberResponse(
                membership.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                membership.isActive() && user.isActive(),
                role.getCode(),
                role.getLabel(),
                membership.getCreatedAt().format(TIMESTAMP_FORMAT),
                actorUserId.equals(user.getId())
        );
    }

    private RoleJpaEntity resolveRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleCode));
    }

    private RoleJpaEntity resolveRoleLabel(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }
}
