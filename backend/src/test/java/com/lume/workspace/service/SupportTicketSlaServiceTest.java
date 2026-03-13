package com.lume.workspace.service;

import com.lume.infrastructure.config.SlaProperties;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SupportTicketSlaServiceTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 6, 1, 12, 0, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            BASE_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private SlaProperties slaProperties;
    private SupportTicketSlaService service;

    @BeforeEach
    void setUp() {
        slaProperties = new SlaProperties();
        // defaults: critical=15, high=60, medium=240, low=1440
        service = new SupportTicketSlaService(slaProperties, FIXED_CLOCK);
    }

    private SupportTicketJpaEntity ticket(String severity, String status, LocalDateTime createdAt) {
        SupportTicketJpaEntity t = new SupportTicketJpaEntity();
        t.setSeverity(severity);
        t.setStatus(status);
        if (createdAt != null) {
            // use reflection to set createdAt since there is no public setter
            try {
                var field = SupportTicketJpaEntity.class.getDeclaredField("createdAt");
                field.setAccessible(true);
                field.set(t, createdAt);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return t;
    }

    @Test
    @DisplayName("Critical severity resolves target at createdAt + 15 minutes")
    void shouldResolveCriticalTargetAt15Min() {
        SupportTicketJpaEntity t = ticket("critical", "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(15));
    }

    @Test
    @DisplayName("High severity resolves target at createdAt + 60 minutes")
    void shouldResolveHighTargetAt60Min() {
        SupportTicketJpaEntity t = ticket("high", "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(60));
    }

    @Test
    @DisplayName("Medium severity resolves target at createdAt + 240 minutes")
    void shouldResolveMediumTargetAt240Min() {
        SupportTicketJpaEntity t = ticket("medium", "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(240));
    }

    @Test
    @DisplayName("Low severity resolves target at createdAt + 1440 minutes")
    void shouldResolveLowTargetAt1440Min() {
        SupportTicketJpaEntity t = ticket("low", "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(1440));
    }

    @Test
    @DisplayName("Null severity defaults to medium (240 minutes)")
    void shouldDefaultToMediumWhenSeverityNull() {
        SupportTicketJpaEntity t = ticket(null, "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(240));
    }

    @Test
    @DisplayName("Unknown severity defaults to medium (240 minutes)")
    void shouldDefaultToMediumWhenSeverityUnknown() {
        SupportTicketJpaEntity t = ticket("xyz", "open", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(240));
    }

    @Test
    @DisplayName("Null createdAt returns null target")
    void shouldReturnNullWhenCreatedAtNull() {
        SupportTicketJpaEntity t = ticket("critical", "open", null);
        assertThat(service.resolveTargetAt(t)).isNull();
    }

    @Test
    @DisplayName("Resolved status returns null target")
    void shouldReturnNullWhenStatusResolved() {
        SupportTicketJpaEntity t = ticket("critical", "resolved", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isNull();
    }

    @Test
    @DisplayName("Closed status returns null target")
    void shouldReturnNullWhenStatusClosed() {
        SupportTicketJpaEntity t = ticket("critical", "closed", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isNull();
    }

    @Test
    @DisplayName("In-progress status returns non-null target")
    void shouldReturnTargetWhenStatusInProgress() {
        SupportTicketJpaEntity t = ticket("critical", "in_progress", BASE_TIME);
        assertThat(service.resolveTargetAt(t)).isNotNull();
    }

    @Test
    @DisplayName("Ticket is breached when clock is past the SLA target")
    void shouldDetectBreachedTicket() {
        // critical = 15min, clock at BASE_TIME which is createdAt + 20min => breached
        LocalDateTime createdAt = BASE_TIME.minusMinutes(20);
        Clock clockAtBase = Clock.fixed(BASE_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        SupportTicketSlaService svc = new SupportTicketSlaService(slaProperties, clockAtBase);

        SupportTicketJpaEntity t = ticket("critical", "open", createdAt);
        assertThat(svc.isBreached(t)).isTrue();
    }

    @Test
    @DisplayName("Ticket is not breached when clock is within SLA")
    void shouldNotBeBreachedWithinSla() {
        // critical = 15min, clock at createdAt + 10min => not breached
        LocalDateTime createdAt = BASE_TIME.minusMinutes(10);
        Clock clockAtBase = Clock.fixed(BASE_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        SupportTicketSlaService svc = new SupportTicketSlaService(slaProperties, clockAtBase);

        SupportTicketJpaEntity t = ticket("critical", "open", createdAt);
        assertThat(svc.isBreached(t)).isFalse();
    }

    @Test
    @DisplayName("Ticket is not breached when target is null (resolved ticket)")
    void shouldNotBeBreachedWhenTargetNull() {
        SupportTicketJpaEntity t = ticket("critical", "resolved", BASE_TIME.minusMinutes(20));
        assertThat(service.isBreached(t)).isFalse();
    }

    @Test
    @DisplayName("Custom SLA properties are respected")
    void shouldRespectCustomSlaProperties() {
        SlaProperties custom = new SlaProperties();
        custom.setCriticalMinutes(30);
        SupportTicketSlaService svc = new SupportTicketSlaService(custom, FIXED_CLOCK);

        SupportTicketJpaEntity t = ticket("critical", "open", BASE_TIME);
        assertThat(svc.resolveTargetAt(t)).isEqualTo(BASE_TIME.plusMinutes(30));
    }
}
