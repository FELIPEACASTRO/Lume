package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.ProviderDefinition;

public interface ProviderReadinessEvaluator {

    String readinessStatus(ProviderDefinition provider);

    String streamingMode(ProviderDefinition provider);

    String runtimeMaturity(ProviderDefinition provider);
}
