package com.lume.workspace.inference.orchestration;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.port.AiProviderAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderRegistry {

    private final Map<ProviderKey, AiProviderAdapter> adaptersByKey;

    public AiProviderRegistry(List<AiProviderAdapter> adapters) {
        this.adaptersByKey = adapters.stream()
                .collect(Collectors.toMap(AiProviderAdapter::providerKey, Function.identity()));
    }

    public AiProviderAdapter require(String providerCode) {
        ProviderKey key = ProviderKey.from(providerCode)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderAdapter", providerCode));
        AiProviderAdapter adapter = adaptersByKey.get(key);
        if (adapter == null) {
            throw new ResourceNotFoundException("ProviderAdapter", providerCode);
        }
        return adapter;
    }

    public boolean supportsStreaming(String providerCode) {
        return ProviderKey.from(providerCode)
                .map(adaptersByKey::get)
                .map(AiProviderAdapter::supportsStreaming)
                .orElse(false);
    }

    public List<String> supportedProviderCodes() {
        return adaptersByKey.keySet().stream()
                .map(ProviderKey::code)
                .toList();
    }
}
