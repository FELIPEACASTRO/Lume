package com.lume.workspace.dto;

public record AgentMessageResponse(
        String id,
        String role,
        String body,
        String timestamp
) {
}
