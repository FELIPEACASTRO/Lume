package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AuditFeedEntryResponse;
import com.lume.workspace.entity.AuditLogJpaEntity;
import com.lume.workspace.repository.AuditLogJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuditLogService - Unit Tests")
class AuditLogServiceTest {

    private AuditLogJpaRepository auditLogRepository;
    private StubWorkspaceContextService workspaceContextService;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogJpaRepository.class);
        workspaceContextService = new StubWorkspaceContextService();
        service = new AuditLogService(auditLogRepository, workspaceContextService, new ObjectMapper());
    }

    @Test
    @DisplayName("record() saves audit entry with payload serialized as JSON")
    void shouldRecordAuditEntryWithSerializedPayload() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = Map.of("key", "value", "count", 42);
        service.record("Provider", "provider-1", "activated", payload);

        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertThat(saved.getOrganizationId()).isEqualTo(1L);
        assertThat(saved.getWorkspaceId()).isEqualTo(1L);
        assertThat(saved.getActorUserId()).isEqualTo(42L);
        assertThat(saved.getEntityType()).isEqualTo("Provider");
        assertThat(saved.getEntityId()).isEqualTo("provider-1");
        assertThat(saved.getAction()).isEqualTo("activated");
        assertThat(saved.getPayload()).contains("\"key\"");
        assertThat(saved.getPayload()).contains("\"value\"");
        assertThat(saved.getPayload()).contains("\"count\"");
        assertThat(saved.getPayload()).contains("42");
    }

    @Test
    @DisplayName("recordExplicit() uses provided orgId, wsId, and userId directly")
    void shouldRecordExplicitWithGivenIds() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordExplicit(99L, 88L, 77L, "Agent", "agent-5", "deleted", null);

        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertThat(saved.getOrganizationId()).isEqualTo(99L);
        assertThat(saved.getWorkspaceId()).isEqualTo(88L);
        assertThat(saved.getActorUserId()).isEqualTo(77L);
        assertThat(saved.getEntityType()).isEqualTo("Agent");
        assertThat(saved.getEntityId()).isEqualTo("agent-5");
        assertThat(saved.getAction()).isEqualTo("deleted");
        assertThat(saved.getPayload()).isNull();
    }

    @Test
    @DisplayName("listCurrentWorkspace() returns mapped responses and respects limit")
    void shouldListCurrentWorkspaceWithLimit() {
        AuditLogJpaEntity entry1 = buildAuditEntry(10L, 42L, "Provider", "p-1", "created",
                "{\"name\":\"OpenAI\"}", LocalDateTime.of(2026, 3, 12, 10, 30, 0));
        AuditLogJpaEntity entry2 = buildAuditEntry(11L, 42L, "Agent", "a-1", "updated",
                null, LocalDateTime.of(2026, 3, 12, 11, 0, 0));

        when(auditLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(List.of(entry1, entry2));

        List<AuditFeedEntryResponse> result = service.listCurrentWorkspace(5);

        assertThat(result).hasSize(2);

        AuditFeedEntryResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(10L);
        assertThat(first.actorUserId()).isEqualTo(42L);
        assertThat(first.entityType()).isEqualTo("Provider");
        assertThat(first.entityId()).isEqualTo("p-1");
        assertThat(first.action()).isEqualTo("created");
        assertThat(first.payload()).isEqualTo("{\"name\":\"OpenAI\"}");
        assertThat(first.createdAt()).isEqualTo("2026-03-12T10:30:00");

        AuditFeedEntryResponse second = result.get(1);
        assertThat(second.id()).isEqualTo(11L);
        assertThat(second.entityType()).isEqualTo("Agent");
        assertThat(second.action()).isEqualTo("updated");
        assertThat(second.payload()).isNull();
        assertThat(second.createdAt()).isEqualTo("2026-03-12T11:00:00");

        // Verify the repository was called with workspace id 1 and page size matching the limit
        verify(auditLogRepository).findByWorkspaceIdOrderByCreatedAtDesc(eq(1L), eq(PageRequest.of(0, 5)));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuditLogJpaEntity buildAuditEntry(Long id, Long actorUserId, String entityType,
                                               String entityId, String action, String payload,
                                               LocalDateTime createdAt) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        setFieldViaReflection(entity, "id", id);
        entity.setOrganizationId(1L);
        entity.setWorkspaceId(1L);
        entity.setActorUserId(actorUserId);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setPayload(payload);
        setFieldViaReflection(entity, "createdAt", createdAt);
        return entity;
    }

    private void setFieldViaReflection(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
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
        @Override public Long getActorUserIdOrNull() { return 42L; }
    }
}
