package com.lume.workspace.inference.orchestration;

public record AiStreamEvent(
        String type,
        String data
) {
}
