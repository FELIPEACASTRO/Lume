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

        ThreatIntelQueryResponse response = unsupported(provider, request.query(), "A integracao deste provedor continua manual/catalog-only nesta rodada.");
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
        List<ThreatIntelExposureDto> previewItems = List.of(
                new ThreatIntelExposureDto(
                        "Execucao manual requerida",
                        provider.name(),
                        "info",
                        "O provider esta catalogado, mas segue sem adapter automatico nesta rodada.",
                        provider.docsUrl()
                )
        );
        return new ThreatIntelQueryResponse(provider.code(), provider.name(), providerCatalogService.isConfigured(provider), provider.executionSupported(), "unsupported", query, previewItems, error);
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
        return justification.trim();
    }
}
