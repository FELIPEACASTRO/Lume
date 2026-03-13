package com.lume.workspace.service;

import com.lume.infrastructure.config.SlaProperties;
import com.lume.workspace.entity.SupportTicketJpaEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SupportTicketSlaService {

    private final SlaProperties slaProperties;
    private final Clock clock;

    public SupportTicketSlaService(SlaProperties slaProperties, Clock clock) {
        this.slaProperties = slaProperties;
        this.clock = clock;
    }

    public LocalDateTime resolveTargetAt(SupportTicketJpaEntity ticket) {
        if (ticket.getCreatedAt() == null || !isOpen(ticket.getStatus())) {
            return null;
        }
        return ticket.getCreatedAt().plusMinutes(minutesForSeverity(ticket.getSeverity()));
    }

    public boolean isBreached(SupportTicketJpaEntity ticket) {
        LocalDateTime targetAt = resolveTargetAt(ticket);
        return targetAt != null && targetAt.isBefore(LocalDateTime.now(clock));
    }

    private boolean isOpen(String status) {
        return "open".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status);
    }

    private long minutesForSeverity(String severity) {
        if (severity == null) {
            return slaProperties.getMediumMinutes();
        }
        return switch (severity.toLowerCase()) {
            case "critical" -> slaProperties.getCriticalMinutes();
            case "high" -> slaProperties.getHighMinutes();
            case "medium" -> slaProperties.getMediumMinutes();
            case "low" -> slaProperties.getLowMinutes();
            default -> slaProperties.getMediumMinutes();
        };
    }
}
