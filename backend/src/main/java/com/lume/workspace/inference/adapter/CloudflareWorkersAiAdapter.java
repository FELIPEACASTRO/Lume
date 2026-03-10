package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.stereotype.Component;

@Component
public class CloudflareWorkersAiAdapter extends DeepSeekAdapter {

    public CloudflareWorkersAiAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.CLOUDFLARE_WORKERS_AI;
    }
}
