package com.lume.workspace.inference.catalog;

import com.lume.workspace.inference.CredentialFieldDefinition;
import com.lume.workspace.inference.InferenceProtocol;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Provider credential and readiness support")
class ProviderCredentialAndReadinessTest {

    @Test
    @DisplayName("should resolve required credentials and replace base URL placeholders only via SecretResolver")
    void shouldResolveRequiredCredentialsAndReplaceBaseUrlPlaceholdersOnlyViaSecretResolver() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("CLOUDFLARE_API_TOKEN", "token-123")
                .withProperty("CLOUDFLARE_ACCOUNT_ID", "account-abc");

        DefaultProviderCredentialInspector inspector =
                new DefaultProviderCredentialInspector(new EnvironmentSecretResolver(environment));

        ProviderDefinition provider = provider(
                "cloudflare-workers-ai",
                true,
                "https://api.cloudflare.com/client/v4/accounts/{CLOUDFLARE_ACCOUNT_ID}/ai/v1",
                List.of(
                        new CredentialFieldDefinition("apiToken", "API Token", "CLOUDFLARE_API_TOKEN", true, true, "token"),
                        new CredentialFieldDefinition("accountId", "Account ID", "CLOUDFLARE_ACCOUNT_ID", true, false, "account")
                ),
                "manual"
        );

        assertThat(inspector.isConfigured(provider)).isTrue();
        assertThat(inspector.missingCredentialEnvVars(provider)).isEmpty();
        assertThat(inspector.credentialValue(provider, "apiToken")).isEqualTo("token-123");
        assertThat(inspector.resolveBaseUrl(provider))
                .isEqualTo("https://api.cloudflare.com/client/v4/accounts/account-abc/ai/v1");
    }

    @Test
    @DisplayName("should expose honest readiness, streaming mode and runtime maturity")
    void shouldExposeHonestReadinessStreamingModeAndRuntimeMaturity() {
        MockEnvironment environment = new MockEnvironment();
        DefaultProviderCredentialInspector inspector =
                new DefaultProviderCredentialInspector(new EnvironmentSecretResolver(environment));
        DefaultProviderReadinessEvaluator evaluator = new DefaultProviderReadinessEvaluator(inspector);

        ProviderDefinition liveProvider = provider(
                "openai",
                true,
                "https://api.openai.com/v1",
                List.of(new CredentialFieldDefinition("apiKey", "API Key", "OPENAI_API_KEY", true, true, "key")),
                "live"
        );
        ProviderDefinition catalogOnlyProvider = provider(
                "darkowl",
                false,
                "https://api.darkowl.com",
                List.of(new CredentialFieldDefinition("publicKey", "Public Key", "DARKOWL_PUBLIC_KEY", true, true, "key")),
                "manual"
        );

        assertThat(evaluator.readinessStatus(liveProvider)).isEqualTo("missing_credentials");
        assertThat(evaluator.streamingMode(liveProvider)).isEqualTo("unsupported");
        assertThat(evaluator.runtimeMaturity(liveProvider)).isEqualTo("live");

        assertThat(evaluator.readinessStatus(catalogOnlyProvider)).isEqualTo("manual");
        assertThat(evaluator.streamingMode(catalogOnlyProvider)).isEqualTo("unsupported");
        assertThat(evaluator.runtimeMaturity(catalogOnlyProvider)).isEqualTo("catalog_only");
    }

    private ProviderDefinition provider(
            String code,
            boolean executionSupported,
            String baseUrlTemplate,
            List<CredentialFieldDefinition> credentialFields,
            String catalogState
    ) {
        return new ProviderDefinition(
                code,
                code,
                "text-runtime",
                InferenceProtocol.OPENAI_RESPONSES,
                executionSupported,
                baseUrlTemplate,
                credentialFields,
                "bearer",
                "responses",
                List.of("Authorization"),
                false,
                true,
                true,
                false,
                catalogState,
                "https://example.com",
                "https://docs.example.com",
                code + ":default",
                List.of("chat"),
                "test"
        );
    }
}
