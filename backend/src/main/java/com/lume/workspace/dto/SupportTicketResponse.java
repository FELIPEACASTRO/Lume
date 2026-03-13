package com.lume.workspace.dto;

public record SupportTicketResponse(
        String id,
        String title,
        String description,
        String category,
        String severity,
        String status,
        String resolutionNote,
        Long createdByUserId,
        String slaTargetAt,
        boolean slaBreached,
        String createdAt,
        String updatedAt
) {
}
