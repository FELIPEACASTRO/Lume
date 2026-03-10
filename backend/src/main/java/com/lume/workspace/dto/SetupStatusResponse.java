package com.lume.workspace.dto;

public record SetupStatusResponse(
        boolean setupRequired,
        long organizations,
        long workspaces,
        long users
) {
}
