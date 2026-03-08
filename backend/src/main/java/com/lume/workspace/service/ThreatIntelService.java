package com.lume.workspace.service;

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

    public ThreatIntelService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
    }

    public ThreatIntelQueryResponse query(ThreatIntelQueryRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_THREAT_INTEL_RUN);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"threat-intel".equalsIgnoreCase(provider.category())) {
            return unsupported(provider, request.query(), "O provedor selecionado nao pertence a categoria de threat-intel.");
        }

        ThreatIntelQueryResponse response = unsupported(provider, request.query(), "A integracao deste provedor continua manual/catalog-only nesta rodada.");
        auditLogService.record(
                "threat_intel_query",
                provider.code(),
                "executed",
                Map.of(
                        "providerCode", provider.code(),
                        "status", response.status(),
                        "query", request.query()
                )
        );
        return response;
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
}
