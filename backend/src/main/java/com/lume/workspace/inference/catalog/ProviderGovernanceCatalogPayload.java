package com.lume.workspace.inference.catalog;

import java.util.Map;

public record ProviderGovernanceCatalogPayload(
        Map<String, ProviderGovernanceMetadata> defaults,
        Map<String, ProviderGovernanceMetadata> providers
) {
}
