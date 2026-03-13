package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.service.PasswordEncoder;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateMemberRequest;
import com.lume.workspace.dto.UpdateMemberRequest;
import com.lume.workspace.entity.MembershipJpaEntity;
import com.lume.workspace.entity.RoleJpaEntity;
import com.lume.workspace.repository.MembershipJpaRepository;
import com.lume.workspace.repository.RoleJpaRepository;
import com.lume.workspace.repository.UserPreferenceJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MemberService - Unit Tests")
class MemberServiceTest {

    private MembershipJpaRepository membershipRepository;
    private RoleJpaRepository roleRepository;
    private JpaUserRepository userRepository;
    private UserPreferenceJpaRepository userPreferenceRepository;
    private PasswordEncoder passwordEncoder;
    private SpyAuditLogService auditLogService;
    private MemberService service;

    @BeforeEach
    void setUp() {
        membershipRepository = mock(MembershipJpaRepository.class);
        roleRepository = mock(RoleJpaRepository.class);
        userRepository = mock(JpaUserRepository.class);
        userPreferenceRepository = mock(UserPreferenceJpaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = new SpyAuditLogService();

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "hashed_" + inv.getArgument(0));
        when(membershipRepository.save(any())).thenAnswer(inv -> {
            MembershipJpaEntity m = inv.getArgument(0);
            if (m.getId() == null) {
                setField(m, "id", 100L);
            }
            triggerPrePersist(m);
            return m;
        });
        when(userPreferenceRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new MemberService(
                new StubWorkspaceContextService(),
                membershipRepository,
                roleRepository,
                userRepository,
                userPreferenceRepository,
                passwordEncoder,
                auditLogService
        );
    }

    @Test
    @DisplayName("listMembers returns mapped MemberResponse list for workspace")
    void shouldListMembersForWorkspace() {
        RoleJpaEntity role = defaultRole();
        UserJpaEntity user = defaultUser(10L, "Ana Silva", "ana@lume.io");

        MembershipJpaEntity membership = new MembershipJpaEntity();
        membership.setUserId(user.getId());
        membership.setOrganizationId(1L);
        membership.setWorkspaceId(1L);
        membership.setRoleId(role.getId());
        membership.setActive(true);
        setField(membership, "id", 100L);
        triggerPrePersist(membership);

        when(membershipRepository.findByWorkspaceIdAndActiveTrueOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(membership));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        var result = service.listMembers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Ana Silva");
        assertThat(result.get(0).email()).isEqualTo("ana@lume.io");
        assertThat(result.get(0).roleCode()).isEqualTo("admin");
        assertThat(result.get(0).roleLabel()).isEqualTo("Administrador");
        assertThat(result.get(0).active()).isTrue();
    }

    @Test
    @DisplayName("createMember creates user and membership, records audit")
    void shouldCreateMemberSuccessfully() {
        RoleJpaEntity role = defaultRole();
        when(roleRepository.findByCode("admin")).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("novo@lume.io")).thenReturn(Optional.empty());
        when(membershipRepository.findByUserIdAndWorkspaceId(10L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        // After user is saved with id=10L, set up findById to return it
        when(userRepository.save(any())).thenAnswer(inv -> {
            UserJpaEntity u = inv.getArgument(0);
            if (u.getId() == null) {
                setField(u, "id", 10L);
            }
            triggerPrePersist(u);
            when(userRepository.findById(10L)).thenReturn(Optional.of(u));
            return u;
        });

        var request = new CreateMemberRequest("Novo Usuario", "novo@lume.io", "senha123", "admin");
        var result = service.createMember(request);

        assertThat(result.name()).isEqualTo("Novo Usuario");
        assertThat(result.email()).isEqualTo("novo@lume.io");
        assertThat(result.roleCode()).isEqualTo("admin");
        assertThat(result.active()).isTrue();
        assertThat(auditLogService.recordedActions).contains("created");
        verify(membershipRepository).save(any());
    }

    @Test
    @DisplayName("createMember throws BusinessRuleException when user already has membership")
    void shouldRejectDuplicateMembership() {
        RoleJpaEntity role = defaultRole();
        when(roleRepository.findByCode("admin")).thenReturn(Optional.of(role));

        UserJpaEntity existingUser = defaultUser(10L, "Existente", "dup@lume.io");
        when(userRepository.findByEmail("dup@lume.io")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MembershipJpaEntity existingMembership = new MembershipJpaEntity();
        setField(existingMembership, "id", 50L);
        existingMembership.setUserId(10L);
        existingMembership.setWorkspaceId(1L);
        triggerPrePersist(existingMembership);
        when(membershipRepository.findByUserIdAndWorkspaceId(10L, 1L))
                .thenReturn(Optional.of(existingMembership));

        var request = new CreateMemberRequest("Existente", "dup@lume.io", "senha123", "admin");

        assertThatThrownBy(() -> service.createMember(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ja possui membership");
    }

    @Test
    @DisplayName("updateMember updates name and email, verifies user and membership saved")
    void shouldUpdateMemberNameAndEmail() {
        RoleJpaEntity role = defaultRole();
        UserJpaEntity user = defaultUser(10L, "Antigo Nome", "antigo@lume.io");

        MembershipJpaEntity membership = new MembershipJpaEntity();
        membership.setUserId(10L);
        membership.setOrganizationId(1L);
        membership.setWorkspaceId(1L);
        membership.setRoleId(1L);
        membership.setActive(true);
        setField(membership, "id", 100L);
        triggerPrePersist(membership);

        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findById(100L)).thenReturn(Optional.of(membership));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("novo@lume.io")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(membershipRepository.findByUserIdAndActiveTrueOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(membership));

        var request = new UpdateMemberRequest("Novo Nome", "novo@lume.io", null, null, null);
        var result = service.updateMember(100L, request);

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(result.email()).isEqualTo("novo@lume.io");
        verify(userRepository, atLeast(1)).save(user);
        verify(membershipRepository).save(membership);
        assertThat(auditLogService.recordedActions).contains("updated");
    }

    @Test
    @DisplayName("updateMember throws BusinessRuleException when actor tries to deactivate themselves")
    void shouldPreventSelfDeactivation() {
        RoleJpaEntity role = defaultRole();
        // Actor user id is 1L (from StubWorkspaceContextService)
        UserJpaEntity actor = defaultUser(1L, "Lume Operator", "operator@lume.io");

        MembershipJpaEntity membership = new MembershipJpaEntity();
        membership.setUserId(1L); // same as actor
        membership.setOrganizationId(1L);
        membership.setWorkspaceId(1L);
        membership.setRoleId(1L);
        membership.setActive(true);
        setField(membership, "id", 200L);
        triggerPrePersist(membership);

        when(membershipRepository.findById(200L)).thenReturn(Optional.of(membership));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        var request = new UpdateMemberRequest(null, null, null, null, false);

        assertThatThrownBy(() -> service.updateMember(200L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desativar a propria membership");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserJpaEntity defaultUser(Long id, String name, String email) {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashed_password");
        user.setActive(true);
        user.setOrganizationId(1L);
        user.setWorkspaceId(1L);
        triggerPrePersist(user);
        return user;
    }

    private RoleJpaEntity defaultRole() {
        RoleJpaEntity role = new RoleJpaEntity();
        setField(role, "id", 1L);
        role.setCode("admin");
        role.setLabel("Administrador");
        role.setDescription("Role de administrador");
        return role;
    }

    private void triggerPrePersist(Object entity) {
        try {
            var method = entity.getClass().getDeclaredMethod("onCreate");
            method.setAccessible(true);
            method.invoke(entity);
        } catch (Exception ignored) {}
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // Try superclass if not found in the declared class
            try {
                Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set field " + fieldName, ex);
            }
        }
    }

    // ── test doubles ────────────────────────────────────────────────────────

    private static final class StubWorkspaceContextService extends WorkspaceContextService {
        private StubWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override public HttpServletRequest getObject(Object... args) { return null; }
                @Override public HttpServletRequest getIfAvailable() { return null; }
                @Override public HttpServletRequest getIfUnique() { return null; }
                @Override public HttpServletRequest getObject() { return null; }
            });
        }

        @Override public void requirePermission(String permission) { }
        @Override public Long getWorkspaceId() { return 1L; }
        @Override public Long getOrganizationId() { return 1L; }
        @Override public Long getActorUserIdOrNull() { return 1L; }
        @Override public String getActorName() { return "Lume Operator"; }
    }

    private static final class SpyAuditLogService extends AuditLogService {
        final List<String> recordedActions = new ArrayList<>();

        private SpyAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }

        @Override
        public void recordExplicit(Long orgId, Long wsId, Long userId,
                                   String entityType, String entityId, String action, Object payload) {
            recordedActions.add(action);
        }
    }
}
