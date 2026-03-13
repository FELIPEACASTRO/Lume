package com.lume.workspace.service;

import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderCatalogRegistry - Unit Tests")
class ProviderCatalogRegistryTest {

    private final ProviderCatalogRegistry registry = new ProviderCatalogRegistry();

    @Test
    @DisplayName("should build all providers with unique codes")
    void shouldBuildAllProvidersWithUniqueCodes() {
        Map<String, ProviderDefinition> providers = registry.getProvidersByCode();

        assertThat(providers).isNotEmpty();
        assertThat(providers.keySet()).contains(
                "openai", "anthropic", "google-gemini", "deepseek", "xai",
                "perplexity", "groq", "mistral", "cohere", "together",
                "exa", "tavily", "voyage-ai", "stability-ai", "replicate",
                "deepgram", "assemblyai", "elevenlabs", "darkowl"
        );
        providers.values().forEach(provider -> {
            assertThat(provider.code()).isNotBlank();
            assertThat(provider.name()).isNotBlank();
            assertThat(provider.category()).isNotBlank();
        });
    }

    @Test
    @DisplayName("should build all models referencing valid providers")
    void shouldBuildAllModelsReferencingValidProviders() {
        Map<String, ModelDefinition> models = registry.getModelsByCode();
        Map<String, ProviderDefinition> providers = registry.getProvidersByCode();

        assertThat(models).isNotEmpty();
        models.values().forEach(model -> {
            assertThat(providers).containsKey(model.providerCode());
            assertThat(model.code()).isNotBlank();
            assertThat(model.label()).isNotBlank();
        });

        Map<String, List<ModelDefinition>> modelsByProvider = registry.getModelsByProviderCode();
        assertThat(modelsByProvider).isNotEmpty();
        modelsByProvider.keySet().forEach(providerCode ->
                assertThat(providers).containsKey(providerCode)
        );
    }

    @Test
    @DisplayName("should resolve aliases to canonical codes")
    void shouldResolveAliasesToCanonicalCodes() {
        Map<String, String> aliases = registry.getAliasesToCanonical();

        assertThat(aliases).containsEntry("gemini", "google-gemini");
        assertThat(aliases).containsEntry("claude", "anthropic");
        assertThat(aliases).containsEntry("grok", "xai");
        assertThat(aliases).containsEntry("voyage", "voyage-ai");
        assertThat(aliases).containsEntry("dashscope", "dashscope-qwen");
    }
}
