package com.lume.workspace.service;

import com.lume.domain.exception.SetupRequiredException;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.dto.OrganizationResponse;
import com.lume.workspace.dto.SessionContextResponse;
import com.lume.workspace.dto.SessionRoleResponse;
import com.lume.workspace.dto.SessionUserResponse;
import com.lume.workspace.dto.SetupStatusResponse;
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
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@Service
public class ApplicationSetupService {

    private final OrganizationJpaRepository organizationRepository;
    private final WorkspaceJpaRepository workspaceRepository;
    private final JpaUserRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final MembershipJpaRepository membershipRepository;
    private final UserPreferenceJpaRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceSessionService workspaceSessionService;
    private final AuditLogService auditLogService;

    public ApplicationSetupService(
            OrganizationJpaRepository organizationRepository,
            WorkspaceJpaRepository workspaceRepository,
            JpaUserRepository userRepository,
            RoleJpaRepository roleRepository,
            MembershipJpaRepository membershipRepository,
            UserPreferenceJpaRepository userPreferenceRepository,
            PasswordEncoder passwordEncoder,
            WorkspaceSessionService workspaceSessionService,
            AuditLogService auditLogService
    ) {
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.workspaceSessionService = workspaceSessionService;
        this.auditLogService = auditLogService;
    }

    public SetupStatusResponse getStatus() {
        return new SetupStatusResponse(
                setupRequired(),
                organizationRepository.count(),
                workspaceRepository.count(),
                userRepository.count()
        );
    }

    @Transactional
    public BootstrapSetupResult bootstrap(BootstrapSetupRequest request, HttpServletRequest httpRequest) {
        if (!setupRequired()) {
            throw new IllegalStateException("O setup inicial ja foi concluido.");
        }

        RoleJpaEntity adminRole = roleRepository.findByCode("workspace_admin")
                .orElseGet(() -> {
                    RoleJpaEntity role = new RoleJpaEntity();
                    role.setCode("workspace_admin");
                    role.setLabel("Workspace Admin");
                    role.setDescription("Acesso administrativo ao workspace");
                    return roleRepository.save(role);
                });
        roleRepository.findByCode("workspace_member")
                .orElseGet(() -> {
                    RoleJpaEntity role = new RoleJpaEntity();
                    role.setCode("workspace_member");
                    role.setLabel("Workspace Member");
                    role.setDescription("Acesso operacional ao workspace");
                    return roleRepository.save(role);
                });

        OrganizationJpaEntity organization = new OrganizationJpaEntity();
        organization.setName(request.organizationName().trim());
        organization.setSlug(uniqueOrganizationSlug(slugify(request.organizationName())));
        organization = organizationRepository.save(organization);

        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity();
        workspace.setOrganizationId(organization.getId());
        workspace.setName(request.workspaceName().trim());
        workspace.setSlug(uniqueWorkspaceSlug(slugify(request.workspaceName())));
        workspace = workspaceRepository.save(workspace);

        UserJpaEntity user = new UserJpaEntity();
        user.setName(request.adminName().trim());
        user.setEmail(request.adminEmail().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(request.password().trim()));
        user.setActive(true);
        user.setOrganizationId(organization.getId());
        user.setWorkspaceId(workspace.getId());
        user = userRepository.save(user);

        MembershipJpaEntity membership = new MembershipJpaEntity();
        membership.setUserId(user.getId());
        membership.setOrganizationId(organization.getId());
        membership.setWorkspaceId(workspace.getId());
        membership.setRoleId(adminRole.getId());
        membership.setActive(true);
        membershipRepository.save(membership);

        UserPreferenceJpaEntity preference = new UserPreferenceJpaEntity();
        preference.setUserId(user.getId());
        preference.setActiveWorkspaceId(workspace.getId());
        preference.setAppearance("light");
        preference.setLanguageCode("pt-BR");
        userPreferenceRepository.save(preference);

        auditLogService.recordExplicit(
                organization.getId(),
                workspace.getId(),
                user.getId(),
                "setup",
                String.valueOf(workspace.getId()),
                "bootstrapped",
                Map.of(
                        "organizationId", organization.getId(),
                        "workspaceId", workspace.getId(),
                        "adminUserId", user.getId()
                )
        );

        ResponseCookie cookie = workspaceSessionService.createSession(user, httpRequest);
        SessionContextResponse session = toSession(user, organization, workspace, adminRole);
        return new BootstrapSetupResult(getStatus(), session, cookie);
    }

    public void requireCompletedSetup() {
        if (setupRequired()) {
            throw new SetupRequiredException("A aplicacao ainda nao concluiu o setup inicial.");
        }
    }

    private boolean setupRequired() {
        return organizationRepository.count() == 0L && workspaceRepository.count() == 0L && userRepository.count() == 0L;
    }

    private String uniqueOrganizationSlug(String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (organizationRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String uniqueWorkspaceSlug(String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (workspaceRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "workspace" : normalized;
    }

    public record BootstrapSetupResult(
            SetupStatusResponse status,
            SessionContextResponse session,
            ResponseCookie cookie
    ) {
    }

    private SessionContextResponse toSession(
            UserJpaEntity user,
            OrganizationJpaEntity organization,
            WorkspaceJpaEntity workspace,
            RoleJpaEntity role
    ) {
        return new SessionContextResponse(
                new SessionUserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        initialsFor(user.getName())
                ),
                new OrganizationResponse(
                        organization.getId(),
                        organization.getName(),
                        organization.getSlug()
                ),
                new WorkspaceResponse(
                        workspace.getId(),
                        workspace.getName(),
                        workspace.getSlug()
                ),
                new SessionRoleResponse(
                        role.getCode(),
                        role.getLabel(),
                        permissionsFor(role.getCode())
                )
        );
    }

    private String initialsFor(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private java.util.List<String> permissionsFor(String roleCode) {
        return switch (roleCode) {
            case "workspace_admin" -> java.util.List.of(
                    WorkspaceContextService.PERMISSION_WORKSPACE_READ,
                    WorkspaceContextService.PERMISSION_WORKSPACE_SWITCH,
                    WorkspaceContextService.PERMISSION_MEMBERS_READ,
                    WorkspaceContextService.PERMISSION_MEMBERS_MANAGE,
                    WorkspaceContextService.PERMISSION_PROVIDERS_READ,
                    WorkspaceContextService.PERMISSION_PROVIDERS_MANAGE,
                    WorkspaceContextService.PERMISSION_PROVIDERS_TEST,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_READ,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_MANAGE,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_READ,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_MANAGE,
                    WorkspaceContextService.PERMISSION_TEMPLATES_READ,
                    WorkspaceContextService.PERMISSION_TEMPLATES_MANAGE,
                    WorkspaceContextService.PERMISSION_BUDGETS_READ,
                    WorkspaceContextService.PERMISSION_BUDGETS_MANAGE,
                    WorkspaceContextService.PERMISSION_SETTINGS_MANAGE,
                    WorkspaceContextService.PERMISSION_AGENTS_RUNTIME_MANAGE,
                    WorkspaceContextService.PERMISSION_RESEARCH_RUN,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_READ,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_MANAGE
            );
            case "workspace_member" -> java.util.List.of(
                    WorkspaceContextService.PERMISSION_WORKSPACE_READ,
                    WorkspaceContextService.PERMISSION_WORKSPACE_SWITCH,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_READ,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_READ,
                    WorkspaceContextService.PERMISSION_TEMPLATES_READ,
                    WorkspaceContextService.PERMISSION_BUDGETS_READ,
                    WorkspaceContextService.PERMISSION_RESEARCH_RUN
            );
            default -> java.util.List.of(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        };
    }
}
