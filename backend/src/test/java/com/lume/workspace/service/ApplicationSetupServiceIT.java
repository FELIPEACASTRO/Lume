package com.lume.workspace.service;

import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.BootstrapSetupRequest;
import com.lume.workspace.dto.SetupStatusResponse;
import com.lume.workspace.entity.OrganizationJpaEntity;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.entity.WorkspaceJpaEntity;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.OrganizationJpaRepository;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import com.lume.workspace.repository.WorkspaceOnboardingProfileJpaRepository;
import com.lume.workspace.repository.WorkspaceSubscriptionJpaRepository;
import com.lume.workspace.repository.WorkspaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "lume.workspace.bootstrap.enabled=false",
        "lume.auth.allow-test-auto-login=false"
})
@ActiveProfiles("test")
class ApplicationSetupServiceIT {

    @Autowired
    private ApplicationSetupService applicationSetupService;
    @Autowired
    private OrganizationJpaRepository organizationRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceRepository;
    @Autowired
    private JpaUserRepository userRepository;
    @Autowired
    private MembershipJpaRepository membershipRepository;
    @Autowired
    private UserPreferenceJpaRepository userPreferenceRepository;
    @Autowired
    private AgentProfileJpaRepository agentProfileRepository;
    @Autowired
    private RoleJpaRepository roleRepository;
    @Autowired
    private WorkspaceOnboardingProfileJpaRepository onboardingRepository;
    @Autowired
    private WorkspaceSubscriptionJpaRepository subscriptionRepository;

    @BeforeEach
    void resetData() {
        membershipRepository.deleteAll();
        userPreferenceRepository.deleteAll();
        agentProfileRepository.deleteAll();
        userRepository.deleteAll();
        workspaceRepository.deleteAll();
        organizationRepository.deleteAll();
        roleRepository.deleteAll();
        onboardingRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    void shouldTreatPartialStateAsSetupRequiredAndReuseExistingOrganizationAndWorkspace() {
        OrganizationJpaEntity organization = new OrganizationJpaEntity();
        organization.setName("Lume");
        organization.setSlug("lume");
        organization = organizationRepository.save(organization);

        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity();
        workspace.setOrganizationId(organization.getId());
        workspace.setName("Workspace Strategy");
        workspace.setSlug("workspace-strategy");
        workspace = workspaceRepository.save(workspace);

        SetupStatusResponse status = applicationSetupService.getStatus();
        assertThat(status.setupRequired()).isTrue();
        assertThat(status.organizations()).isEqualTo(1);
        assertThat(status.workspaces()).isEqualTo(1);
        assertThat(status.users()).isZero();

        BootstrapSetupRequest request = new BootstrapSetupRequest(
                "Lume",
                "Workspace Principal",
                "Lume Operator",
                "operator@lume.local",
                "lume12345",
                "operations",
                "small_team",
                "core"
        );

        ApplicationSetupService.BootstrapSetupResult result =
                applicationSetupService.bootstrap(request, new MockHttpServletRequest());

        assertThat(result.session().user().email()).isEqualTo("operator@lume.local");
        assertThat(result.session().workspace().id()).isEqualTo(workspace.getId());
        assertThat(result.session().workspace().name()).isEqualTo("Workspace Principal");
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(membershipRepository.count()).isEqualTo(1);
        assertThat(agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspace.getId()))
                .hasSize(1)
                .first()
                .satisfies(profile -> {
                    assertThat(profile.getProviderCode()).isEqualTo("openai");
                    assertThat(profile.getModelCode()).isEqualTo("openai:gpt-4.1-mini");
                });
        assertThat(onboardingRepository.findByWorkspaceId(workspace.getId()))
                .isPresent()
                .get()
                .satisfies(onboarding -> {
                    assertThat(onboarding.getPrimaryUseCase()).isEqualTo("operations");
                    assertThat(onboarding.getWorkStyle()).isEqualTo("small_team");
                });
        assertThat(subscriptionRepository.findByWorkspaceId(workspace.getId()))
                .isPresent()
                .get()
                .satisfies(subscription -> {
                    assertThat(subscription.getPlanCode()).isEqualTo("core");
                    assertThat(subscription.getIncludedCredits()).isEqualTo(1500);
                    assertThat(subscription.getSubscriptionStatus()).isEqualTo("active");
                });

        RoleJpaEntity adminRole = roleRepository.findByCode("workspace_admin").orElseThrow();
        assertThat(adminRole.getLabel()).isEqualTo("Workspace Admin");
    }
}
