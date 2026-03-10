package com.lume.workspace.dto;

import java.util.List;

public record AgentConversationResponse(
        AgentThreadResponse thread,
        List<AgentMessageResponse> messages
) {
}
