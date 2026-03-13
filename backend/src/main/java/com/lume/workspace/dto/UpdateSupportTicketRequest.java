package com.lume.workspace.dto;

import jakarta.validation.constraints.Size;

public record UpdateSupportTicketRequest(
        @Size(max = 40) String status,
        @Size(max = 2000) String resolutionNote,
        @Size(max = 40) String severity,
        @Size(max = 40) String category
) {
}
