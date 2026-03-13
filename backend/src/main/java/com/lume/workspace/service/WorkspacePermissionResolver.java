package com.lume.workspace.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pure-logic service that maps role codes to their granted permissions.
 * Extracted from {@link WorkspaceContextService} to honour the
 * Single-Responsibility and Open-Closed principles.
 */
@Service
public class WorkspacePermissionResolver {

    public List<String> permissionsFor(String roleCode) {
        return switch (roleCode) {
            case "workspace_admin" -> List.of(
                    WorkspaceContextService.PERMISSION_WORKSPACE_READ,
                    WorkspaceContextService.PERMISSION_WORKSPACE_SWITCH,
                    WorkspaceContextService.PERMISSION_MEMBERS_READ,
                    WorkspaceContextService.PERMISSION_MEMBERS_MANAGE,
                    WorkspaceContextService.PERMISSION_PROVIDERS_READ,
                    WorkspaceContextService.PERMISSION_PROVIDERS_MANAGE,
                    WorkspaceContextService.PERMISSION_PROVIDERS_TEST,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_READ,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_MANAGE,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_READ,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_MANAGE,
                    WorkspaceContextService.PERMISSION_TEMPLATES_READ,
                    WorkspaceContextService.PERMISSION_TEMPLATES_MANAGE,
                    WorkspaceContextService.PERMISSION_BUDGETS_READ,
                    WorkspaceContextService.PERMISSION_BUDGETS_MANAGE,
                    WorkspaceContextService.PERMISSION_SETTINGS_MANAGE,
                    WorkspaceContextService.PERMISSION_AGENTS_RUNTIME_MANAGE,
                    WorkspaceContextService.PERMISSION_RESEARCH_RUN,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_READ,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN,
                    WorkspaceContextService.PERMISSION_THREAT_INTEL_MANAGE
            );
            case "workspace_member" -> List.of(
                    WorkspaceContextService.PERMISSION_WORKSPACE_READ,
                    WorkspaceContextService.PERMISSION_WORKSPACE_SWITCH,
                    WorkspaceContextService.PERMISSION_KNOWLEDGE_READ,
                    WorkspaceContextService.PERMISSION_ARTIFACTS_READ,
                    WorkspaceContextService.PERMISSION_TEMPLATES_READ,
                    WorkspaceContextService.PERMISSION_BUDGETS_READ,
                    WorkspaceContextService.PERMISSION_RESEARCH_RUN
            );
            default -> List.of(WorkspaceContextService.PERMISSION_WORKSPACE_READ);
        };
    }
}
