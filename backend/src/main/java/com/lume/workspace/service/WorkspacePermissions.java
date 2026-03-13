package com.lume.workspace.service;

/**
 * Centralized permission constants for workspace RBAC.
 */
public final class WorkspacePermissions {

    private WorkspacePermissions() {
    }

    public static final String WORKSPACE_READ = "workspace.read";
    public static final String WORKSPACE_SWITCH = "workspace.switch";
    public static final String MEMBERS_READ = "members.read";
    public static final String MEMBERS_MANAGE = "members.manage";
    public static final String PROVIDERS_READ = "providers.read";
    public static final String PROVIDERS_MANAGE = "providers.manage";
    public static final String PROVIDERS_TEST = "providers.test";
    public static final String AGENTS_RUNTIME_MANAGE = "agents.runtime.manage";
    public static final String RESEARCH_RUN = "research.run";
    public static final String KNOWLEDGE_READ = "knowledge.read";
    public static final String KNOWLEDGE_MANAGE = "knowledge.manage";
    public static final String ARTIFACTS_READ = "artifacts.read";
    public static final String ARTIFACTS_MANAGE = "artifacts.manage";
    public static final String TEMPLATES_READ = "templates.read";
    public static final String TEMPLATES_MANAGE = "templates.manage";
    public static final String BUDGETS_READ = "budgets.read";
    public static final String BUDGETS_MANAGE = "budgets.manage";
    public static final String SETTINGS_MANAGE = "settings.manage";
    public static final String THREAT_INTEL_READ = "threat_intel.read";
    public static final String THREAT_INTEL_RUN = "threat_intel.run";
    public static final String THREAT_INTEL_MANAGE = "threat_intel.manage";
}
