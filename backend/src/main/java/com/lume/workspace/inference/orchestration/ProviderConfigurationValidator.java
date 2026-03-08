package com.lume.workspace.inference.orchestration;

import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.service.ProviderCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProviderConfigurationValidator implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderConfigurationValidator.class);

    private final ProviderCatalogService providerCatalogService;
    private final AiProviderRegistry providerRegistry;

    public ProviderConfigurationValidator(
            ProviderCatalogService providerCatalogService,
            AiProviderRegistry providerRegistry
    ) {
        this.providerCatalogService = providerCatalogService;
        this.providerRegistry = providerRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        providerCatalogService.listProviderStatuses().stream()
                .filter(status -> providerRegistry.supportedProviderCodes().contains(providerCatalogService.normalizeProviderCode(status.providerCode())))
                .forEach(this::logStatus);
    }

    private void logStatus(ProviderStatusResponse status) {
        String state = status.configured()
                ? "configurado"
                : status.missingCredentialEnvVars().size() == 1 ? "sem credencial" : "parcial";
        LOGGER.info(
                "Provider {} ({}) iniciou com estado={} streamingMode={} runtimeMaturity={} faltando={}",
                status.providerName(),
                status.providerCode(),
                state,
                status.streamingMode(),
                status.runtimeMaturity(),
                status.missingCredentialEnvVars()
        );
    }
}
