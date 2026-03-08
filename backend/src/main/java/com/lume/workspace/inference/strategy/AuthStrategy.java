package com.lume.workspace.inference.strategy;

import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.service.ProviderCatalogService;

import java.util.Map;

@FunctionalInterface
public interface AuthStrategy {

    Map<String, String> headers(ProviderDefinition provider, ProviderCatalogService providerCatalogService);
}
