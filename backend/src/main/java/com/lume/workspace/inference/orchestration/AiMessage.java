package com.lume.workspace.inference.orchestration;

public record AiMessage(
        String role,
        String content
) {
}
