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
    @DisplayName("GET /api/v1/provider-credentials - should expose credential env vars without leaking values")
    void shouldReturnCredentialHints() throws Exception {
        mockMvc.perform(get("/api/v1/provider-credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].apiKeyEnvVar").value("OPENAI_API_KEY"))
                .andExpect(jsonPath("$[?(@.providerCode=='openai')].configured").value(false));
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
                .andExpect(jsonPath("$.configured").value(false));
    }
}
