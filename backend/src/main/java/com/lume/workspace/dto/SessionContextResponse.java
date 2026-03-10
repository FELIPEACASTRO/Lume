package com.lume.workspace.dto;

public record SessionContextResponse(
        SessionUserResponse user,
        OrganizationResponse organization,
        WorkspaceResponse workspace,
        SessionRoleResponse role
) {
}
