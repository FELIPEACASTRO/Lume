package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("InferenceGatewayService - Unit Tests")
class InferenceGatewayServiceTest {

    @Test
    @DisplayName("should execute OpenAI-compatible providers")
    void shouldExecuteOpenAiCompatibleProviders() {
        MockEnvironment environment = new MockEnvironment().withProperty("OPENAI_API_KEY", "test-openai");
        ProviderCatalogService catalogService = new ProviderCatalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Resposta OpenAI"}}]}
                        """, MediaType.APPLICATION_JSON));

        InferenceGatewayService service = new InferenceGatewayService(builder, new ObjectMapper(), catalogService, environment);
        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "openai",
                null,
                "Seja direto.",
                "Resuma o estado do workspace.",
                null,
                0.2,
                200
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.content()).isEqualTo("Resposta OpenAI");
        server.verify();
    }

    @Test
    @DisplayName("should execute Anthropic messages protocol")
    void shouldExecuteAnthropicProtocol() {
        MockEnvironment environment = new MockEnvironment().withProperty("ANTHROPIC_API_KEY", "test-anthropic");
        ProviderCatalogService catalogService = new ProviderCatalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-anthropic"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"Resposta Claude"}]}
                        """, MediaType.APPLICATION_JSON));

        InferenceGatewayService service = new InferenceGatewayService(builder, new ObjectMapper(), catalogService, environment);
        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "anthropic",
                null,
                "Seja conciso.",
                "Explique o backlog.",
                null,
                0.2,
                200
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.content()).isEqualTo("Resposta Claude");
        server.verify();
    }

    @Test
    @DisplayName("should execute Gemini generate content protocol")
    void shouldExecuteGeminiProtocol() {
        MockEnvironment environment = new MockEnvironment().withProperty("GEMINI_API_KEY", "test-gemini");
        ProviderCatalogService catalogService = new ProviderCatalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=test-gemini"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Resposta Gemini"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        InferenceGatewayService service = new InferenceGatewayService(builder, new ObjectMapper(), catalogService, environment);
        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "google-gemini",
                null,
                "Seja objetivo.",
                "Mostre os proximos passos.",
                null,
                0.2,
                200
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.content()).isEqualTo("Resposta Gemini");
        server.verify();
    }

    @Test
    @DisplayName("should return missing credentials when provider key is absent")
    void shouldReturnMissingCredentialsWhenProviderKeyIsAbsent() {
        MockEnvironment environment = new MockEnvironment();
        ProviderCatalogService catalogService = new ProviderCatalogService(environment);
        InferenceGatewayService service = new InferenceGatewayService(RestClient.builder(), new ObjectMapper(), catalogService, environment);

        UnifiedInferenceResponse response = service.execute(new UnifiedInferenceRequest(
                "openai",
                null,
                null,
                "Hello",
                null,
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("missing_credentials");
        assertThat(response.configured()).isFalse();
        assertThat(response.content()).isNull();
    }
}
