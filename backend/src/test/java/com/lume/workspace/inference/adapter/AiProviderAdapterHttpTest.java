package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.inference.orchestration.AiMessage;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import com.lume.workspace.service.ProviderCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AI Provider Adapters - Unit Tests")
class AiProviderAdapterHttpTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("OpenAI adapter should call Responses API")
    void shouldCallOpenAiResponsesApi() {
        MockEnvironment environment = new MockEnvironment().withProperty("OPENAI_API_KEY", "test-openai");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai"))
                .andRespond(withSuccess("""
                        {"output":[{"content":[{"type":"output_text","text":"Resposta OpenAI"}]}]}
                        """, MediaType.APPLICATION_JSON));

        OpenAiAdapter adapter = new OpenAiAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("openai", "Resuma o workspace."));

        assertThat(result.providerCode()).isEqualTo("openai");
        assertThat(result.content()).isEqualTo("Resposta OpenAI");
    }

    @Test
    @DisplayName("Gemini adapter should call generateContent")
    void shouldCallGeminiGenerateContent() {
        MockEnvironment environment = new MockEnvironment().withProperty("GEMINI_API_KEY", "test-gemini");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"Resposta Gemini"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        GeminiAdapter adapter = new GeminiAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("google-gemini", "Liste os proximos passos."));

        assertThat(result.providerCode()).isEqualTo("google-gemini");
        assertThat(result.content()).isEqualTo("Resposta Gemini");
    }

    @Test
    @DisplayName("DeepSeek adapter should call chat completions")
    void shouldCallDeepSeekChatCompletions() {
        MockEnvironment environment = new MockEnvironment().withProperty("DEEPSEEK_API_KEY", "test-deepseek");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-deepseek"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Resposta DeepSeek"}}]}
                        """, MediaType.APPLICATION_JSON));

        DeepSeekAdapter adapter = new DeepSeekAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("deepseek", "Explique a prioridade."));

        assertThat(result.providerCode()).isEqualTo("deepseek");
        assertThat(result.content()).isEqualTo("Resposta DeepSeek");
    }

    @Test
    @DisplayName("Anthropic adapter should call Messages API")
    void shouldCallAnthropicMessagesApi() {
        MockEnvironment environment = new MockEnvironment().withProperty("ANTHROPIC_API_KEY", "test-anthropic");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-anthropic"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"Resposta Claude"}]}
                        """, MediaType.APPLICATION_JSON));

        AnthropicAdapter adapter = new AnthropicAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("anthropic", "Revise o plano."));

        assertThat(result.providerCode()).isEqualTo("anthropic");
        assertThat(result.content()).isEqualTo("Resposta Claude");
    }

    @Test
    @DisplayName("xAI adapter should call Responses API")
    void shouldCallXaiResponsesApi() {
        MockEnvironment environment = new MockEnvironment().withProperty("XAI_API_KEY", "test-xai");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.x.ai/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-xai"))
                .andRespond(withSuccess("""
                        {"output":[{"content":[{"type":"output_text","text":"Resposta Grok"}]}]}
                        """, MediaType.APPLICATION_JSON));

        XaiAdapter adapter = new XaiAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("xai", "Leia os sinais."));

        assertThat(result.providerCode()).isEqualTo("xai");
        assertThat(result.content()).isEqualTo("Resposta Grok");
    }

    @Test
    @DisplayName("Perplexity adapter should call Sonar via chat completions")
    void shouldCallPerplexityChatCompletions() {
        MockEnvironment environment = new MockEnvironment().withProperty("PERPLEXITY_API_KEY", "test-pplx");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.perplexity.ai/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-pplx"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Resposta Sonar"}}]}
                        """, MediaType.APPLICATION_JSON));

        PerplexityAdapter adapter = new PerplexityAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("perplexity", "Traga o briefing."));

        assertThat(result.providerCode()).isEqualTo("perplexity");
        assertThat(result.content()).isEqualTo("Resposta Sonar");
    }

    @Test
    @DisplayName("Groq adapter should call responses endpoint")
    void shouldCallGroqResponsesApi() {
        MockEnvironment environment = new MockEnvironment().withProperty("GROQ_API_KEY", "test-groq");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-groq"))
                .andRespond(withSuccess("""
                        {"output":[{"content":[{"type":"output_text","text":"Resposta Groq"}]}]}
                        """, MediaType.APPLICATION_JSON));

        GroqAdapter adapter = new GroqAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("groq", "Responda com uma linha."));

        assertThat(result.providerCode()).isEqualTo("groq");
        assertThat(result.content()).isEqualTo("Resposta Groq");
    }

    @Test
    @DisplayName("OpenRouter adapter should call chat completions endpoint")
    void shouldCallOpenRouterChatCompletions() {
        MockEnvironment environment = new MockEnvironment().withProperty("OPENROUTER_API_KEY", "test-openrouter");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openrouter"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Resposta OpenRouter"}}]}
                        """, MediaType.APPLICATION_JSON));

        OpenRouterAdapter adapter = new OpenRouterAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("openrouter", "Responda com uma linha."));

        assertThat(result.providerCode()).isEqualTo("openrouter");
        assertThat(result.content()).isEqualTo("Resposta OpenRouter");
    }

    @Test
    @DisplayName("Cohere adapter should call chat v2 endpoint")
    void shouldCallCohereChatApi() {
        MockEnvironment environment = new MockEnvironment().withProperty("COHERE_API_KEY", "test-cohere");
        ProviderCatalogService catalogService = catalogService(environment);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.cohere.com/v2/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-cohere"))
                .andRespond(withSuccess("""
                        {"message":{"content":[{"type":"text","text":"Resposta Cohere"}]}}
                        """, MediaType.APPLICATION_JSON));

        CohereAdapter adapter = new CohereAdapter(catalogService, new AiHttpExecutor(builder, objectMapper), new AiRuntimeProperties(), objectMapper);
        AiPromptResult result = adapter.sendPrompt(command("cohere", "Resuma em uma linha."));

        assertThat(result.providerCode()).isEqualTo("cohere");
        assertThat(result.content()).isEqualTo("Resposta Cohere");
    }

    private AiPromptCommand command(String providerCode, String prompt) {
        return new AiPromptCommand(
                "req-test-" + providerCode,
                providerCode,
                providerCode,
                null,
                "agent-v1",
                "Seja direto.",
                List.of(new AiMessage("user", prompt)),
                0.2,
                120
        );
    }

    private ProviderCatalogService catalogService(MockEnvironment environment) {
        return new ProviderCatalogService(new EnvironmentSecretResolver(environment));
    }
}
