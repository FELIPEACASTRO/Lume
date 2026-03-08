package com.lume.workspace.inference.orchestration;

import java.time.OffsetDateTime;

public record ProviderConnectivitySnapshot(
        String providerCode,
        String status,
        String message,
        OffsetDateTime checkedAt
) {
}
