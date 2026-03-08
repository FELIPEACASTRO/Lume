package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Provider Catalog - Integration Tests")
class ProviderCatalogIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/providers - should return AI and search providers")
    void shouldReturnProviders() throws Exception {
        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='openai')]").exists())
                .andExpect(jsonPath("$[?(@.code=='anthropic')]").exists())
                .andExpect(jsonPath("$[?(@.code=='exa')]").exists())
                .andExpect(jsonPath("$[?(@.code=='voyage-ai')]").exists())
                .andExpect(jsonPath("$[?(@.code=='stability-ai')]").exists())
                .andExpect(jsonPath("$[?(@.code=='replicate')]").exists())
                .andExpect(jsonPath("$[?(@.code=='deepgram')]").exists())
                .andExpect(jsonPath("$[?(@.code=='assemblyai')]").exists())
                .andExpect(jsonPath("$[?(@.code=='elevenlabs')]").exists())
                .andExpect(jsonPath("$[?(@.code=='ideogram')]").exists())
                .andExpect(jsonPath("$[?(@.code=='bfl')]").exists())
                .andExpect(jsonPath("$[?(@.code=='runway')]").exists())
                .andExpect(jsonPath("$[?(@.code=='tavily')]").exists())
                .andExpect(jsonPath("$[?(@.code=='darkowl')]").exists());
    }

    @Test
    @DisplayName("GET /api/v1/models?provider=openai - should return provider model catalog")
    void shouldReturnModelsByProvider() throws Exception {
        mockMvc.perform(get("/api/v1/models").param("provider", "openai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerCode").value("openai"))
                .andExpect(jsonPath("$[0].code").value("openai:gpt-4.1-mini"));
    }

    @Test
    @DisplayName("GET /api/v1/providers/openrouter - should return provider detail")
    void shouldReturnProviderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/providers/openrouter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("openrouter"))
                .andExpect(jsonPath("$.executionSupported").value(true))
                .andExpect(jsonPath("$.catalogState").value("live"))
                .andExpect(jsonPath("$.implementationStatus").value("implemented_with_restrictions"))
                .andExpect(jsonPath("$.evidenceLevel").value("integration_verified"))
                .andExpect(jsonPath("$.pricingSummary").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/providers/openrouter/models - should return scoped models")
    void shouldReturnModelsForProviderPath() throws Exception {
        mockMvc.perform(get("/api/v1/providers/openrouter/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerCode=='openrouter')]").exists());
    }

    @Test
    @DisplayName("GET /api/v1/provider-credentials - should expose credential env vars without leaking values")
    void shouldReturnCredentialHints() throws Exception {
        mockMvc.perform(get("/api/v1/provider-credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].credentialFields[0].envVar").value("OPENAI_API_KEY"))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].configured").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/providers/status - should expose configuration state")
    void shouldReturnProviderStatuses() throws Exception {
        mockMvc.perform(get("/api/v1/providers/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].category", hasItem("text-runtime")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].implementationStatus", hasItem("live")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].evidenceLevel", hasItem("integration_verified")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].streamingMode", hasItem("unsupported")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].runtimeMaturity", hasItem("live")))
                .andExpect(jsonPath("$[?(@.providerCode=='openrouter')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='deepgram')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='deepgram')].implementationStatus", hasItem("implemented_with_restrictions")))
                .andExpect(jsonPath("$[?(@.providerCode=='assemblyai')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='assemblyai')].implementationStatus", hasItem("implemented_with_restrictions")))
                .andExpect(jsonPath("$[?(@.providerCode=='elevenlabs')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='stability-ai')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='stability-ai')].implementationStatus", hasItem("implemented_with_restrictions")))
                .andExpect(jsonPath("$[?(@.providerCode=='replicate')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='replicate')].implementationStatus", hasItem("implemented_with_restrictions")))
                .andExpect(jsonPath("$[?(@.providerCode=='ideogram')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='bfl')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='bfl')].implementationStatus", hasItem("implemented_with_restrictions")))
                .andExpect(jsonPath("$[?(@.providerCode=='runway')].executionSupported", hasItem(true)))
                .andExpect(jsonPath("$[?(@.providerCode=='darkowl')].adminOnly", hasItem(true)));
    }

    @Test
    @DisplayName("GET /api/v1/providers/health - should expose readiness and last connectivity state")
    void shouldReturnProviderHealth() throws Exception {
        mockMvc.perform(get("/api/v1/providers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].implementationStatus", hasItem("live")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].evidenceLevel", hasItem("integration_verified")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].readinessStatus", hasItem("missing_credentials")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].streamingMode", hasItem("unsupported")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].healthSource", hasItem("static")))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].snapshotPersistence", hasItem("memory")));
    }

    @Test
    @DisplayName("GET /api/v1/research/providers - should expose research providers only")
    void shouldReturnResearchProviders() throws Exception {
        mockMvc.perform(get("/api/v1/research/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='exa')]").exists())
                .andExpect(jsonPath("$[?(@.code=='tavily')]").exists())
                .andExpect(jsonPath("$[?(@.code=='serpapi')]").exists())
                .andExpect(jsonPath("$[?(@.code=='darkowl')]").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/inference/execute - should report missing credentials in test profile")
    void shouldReportMissingCredentialsInTestProfile() throws Exception {
        UnifiedInferenceRequest request = new UnifiedInferenceRequest(
                "openai",
                null,
                null,
                "Resuma o estado do workspace.",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/inference/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("openai"))
                .andExpect(jsonPath("$.status").value("missing_credentials"))
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.apiStyle").value("responses"))
                .andExpect(jsonPath("$.requestedProviderCode").value("openai"))
                .andExpect(jsonPath("$.streamingMode").value("unsupported"));
    }
}
