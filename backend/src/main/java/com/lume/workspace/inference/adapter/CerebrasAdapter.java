package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.stereotype.Component;

@Component
public class CerebrasAdapter extends DeepSeekAdapter {

    public CerebrasAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.CEREBRAS;
    }
}
