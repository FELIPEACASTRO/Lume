package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AI Platform Controller - Integration Tests")
class AiPlatformControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/chat - should reuse unified inference runtime")
    void shouldExecuteChatThroughUnifiedRuntime() throws Exception {
        AiPlatformModels.ChatRequest request = new AiPlatformModels.ChatRequest(
                "openai",
                null,
                "Seja direto.",
                "Resuma o workspace.",
                null,
                0.2,
                200,
                java.util.List.of("anthropic"),
                false,
                "req-chat"
        );

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("openai"))
                .andExpect(jsonPath("$.status").value("missing_credentials"))
                .andExpect(jsonPath("$.streamingSupported").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/search - should expose missing credentials for live research provider")
    void shouldReturnMissingCredentialsForSearchProvider() throws Exception {
        AiPlatformModels.AiSearchRequest request = new AiPlatformModels.AiSearchRequest(
                "newscatcher",
                "mercado de IA",
                5
        );

        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("newscatcher"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/threat-intel/search - should block when compliance flag is disabled")
    void shouldBlockThreatIntelWhenComplianceFlagIsDisabled() throws Exception {
        ThreatIntelQueryRequest request = new ThreatIntelQueryRequest(
                "darkowl",
                "example onion marketplace",
                5,
                "Incidente interno sob analise"
        );

        mockMvc.perform(post("/api/v1/threat-intel/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("darkowl"))
                .andExpect(jsonPath("$.status").value("compliance_blocked"));
    }

    @Test
    @DisplayName("POST /api/v1/responses - should reject openrouter free tier without :free model suffix")
    void shouldValidateOpenRouterFreeTierModelSuffix() throws Exception {
        AiPlatformModels.ResponseRequest request = new AiPlatformModels.ResponseRequest(
                "openrouter",
                "openrouter:openai/gpt-4.1-mini",
                null,
                "Responda apenas ok.",
                null,
                0.1,
                64,
                java.util.List.of(),
                true,
                "req-openrouter-free"
        );

        mockMvc.perform(post("/api/v1/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OpenRouter em modo gratuito exige modelCode com sufixo :free."));
    }
}
