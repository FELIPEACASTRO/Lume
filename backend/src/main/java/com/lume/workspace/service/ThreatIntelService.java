package com.lume.workspace.service;

import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.ThreatIntelExposureDto;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ThreatIntelService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final SecurityComplianceProperties securityComplianceProperties;

    public ThreatIntelService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            SecurityComplianceProperties securityComplianceProperties
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.securityComplianceProperties = securityComplianceProperties;
    }

    public ThreatIntelQueryResponse query(ThreatIntelQueryRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"threat-intel".equalsIgnoreCase(provider.category())) {
            return unsupported(provider, request.query(), "O provedor selecionado nao pertence a categoria de threat-intel.");
        }

        if (!securityComplianceProperties.isDarkWebEnabled()) {
            ThreatIntelQueryResponse blocked = blocked(provider, request.query(), "A feature flag security.compliance.dark-web-enabled esta desligada.");
            auditLogService.record(
                    "threat_intel_query",
                    provider.code(),
                    "blocked",
                    Map.of(
                            "providerCode", provider.code(),
                            "status", blocked.status(),
                            "queryPreview", redactQuery(request.query()),
                            "justification", safeJustification(request.justification())
                    )
            );
            return blocked;
        }

        if (request.justification() == null || request.justification().isBlank()) {
            ThreatIntelQueryResponse invalid = blocked(provider, request.query(), "Uma justificativa e obrigatoria para consultas de threat-intel.");
            auditLogService.record(
                    "threat_intel_query",
                    provider.code(),
                    "blocked",
                    Map.of(
                            "providerCode", provider.code(),
                            "status", invalid.status(),
                            "queryPreview", redactQuery(request.query()),
                            "justification", safeJustification(request.justification())
                    )
            );
            return invalid;
        }

        ThreatIntelQueryResponse response = unsupported(provider, request.query(), "Esta integracao ainda exige habilitacao manual antes do uso operacional.");
        auditLogService.record(
                "threat_intel_query",
                provider.code(),
                "executed",
                Map.of(
                        "providerCode", provider.code(),
                        "status", response.status(),
                        "queryPreview", redactQuery(request.query()),
                        "justification", safeJustification(request.justification())
                )
        );
        return response;
    }

    private ThreatIntelQueryResponse blocked(ProviderDefinition provider, String query, String error) {
        return new ThreatIntelQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "compliance_blocked", query, List.of(), error);
    }

    private ThreatIntelQueryResponse unsupported(ProviderDefinition provider, String query, String error) {
        return new ThreatIntelQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "unsupported", query, List.of(), error);
    }

    private String redactQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        int visible = Math.min(6, query.length());
        return query.substring(0, visible) + "***";
    }

    private String safeJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            return "missing";
        }
        String normalized = justification.trim().replaceAll("\\s+", " ");
        int previewLength = Math.min(24, normalized.length());
        return normalized.substring(0, previewLength) + (normalized.length() > previewLength ? "***" : "");
    }
}
