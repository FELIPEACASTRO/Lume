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
                .andExpect(jsonPath("$.requestedProviderCode").value("openai"))
                .andExpect(jsonPath("$.providerUsed").value("openai"))
                .andExpect(jsonPath("$.streamingMode").value("unsupported"))
                .andExpect(jsonPath("$.streamingSupported").value(false))
                .andExpect(jsonPath("$.attemptChain[0].status").value("missing_credentials"));
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
    @DisplayName("POST /api/v1/embeddings - should expose missing credentials for voyage ai")
    void shouldReturnMissingCredentialsForEmbeddingsProvider() throws Exception {
        AiPlatformModels.EmbeddingRequest request = new AiPlatformModels.EmbeddingRequest(
                "voyage-ai",
                null,
                "documento base"
        );

        mockMvc.perform(post("/api/v1/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("voyage-ai"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/rerank - should expose missing credentials for cohere")
    void shouldReturnMissingCredentialsForRerankProvider() throws Exception {
        AiPlatformModels.RerankRequest request = new AiPlatformModels.RerankRequest(
                "cohere",
                null,
                "qual item fala de roadmap?",
                java.util.List.of("roadmap do produto", "plano financeiro"),
                2
        );

        mockMvc.perform(post("/api/v1/rerank")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("cohere"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - should expose missing credentials for ideogram capability runtime")
    void shouldReturnMissingCredentialsForImageGenerationProvider() throws Exception {
        AiPlatformModels.ImageGenerationRequest request = new AiPlatformModels.ImageGenerationRequest(
                "ideogram",
                null,
                "Poster minimalista de um foguete.",
                "1:1",
                "DESIGN",
                1,
                null
        );

        mockMvc.perform(post("/api/v1/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("ideogram"))
                .andExpect(jsonPath("$.modelCode").value("ideogram:v3"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/edit - should expose missing credentials for ideogram image editing runtime")
    void shouldReturnMissingCredentialsForImageEditingProvider() throws Exception {
        AiPlatformModels.ImageEditRequest request = new AiPlatformModels.ImageEditRequest(
                "ideogram",
                null,
                "Substitua o fundo por um estudio escuro.",
                "https://assets.example.com/input.png",
                "https://assets.example.com/mask.png"
        );

        mockMvc.perform(post("/api/v1/images/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("ideogram"))
                .andExpect(jsonPath("$.modelCode").value("ideogram:v3"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - should expose missing credentials for BFL capability runtime")
    void shouldReturnMissingCredentialsForBflImageGenerationProvider() throws Exception {
        AiPlatformModels.ImageGenerationRequest request = new AiPlatformModels.ImageGenerationRequest(
                "bfl",
                null,
                "Ilustracao editorial em luz dura.",
                "1:1",
                null,
                1,
                null
        );

        mockMvc.perform(post("/api/v1/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("bfl"))
                .andExpect(jsonPath("$.modelCode").value("bfl:flux-2-pro"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - should expose missing credentials for Stability AI capability runtime")
    void shouldReturnMissingCredentialsForStabilityImageGenerationProvider() throws Exception {
        AiPlatformModels.ImageGenerationRequest request = new AiPlatformModels.ImageGenerationRequest(
                "stability-ai",
                null,
                "Poster minimalista de produto premium.",
                "1:1",
                null,
                1,
                null
        );

        mockMvc.perform(post("/api/v1/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("stability-ai"))
                .andExpect(jsonPath("$.modelCode").value("stability-ai:stable-image-core"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - should expose missing credentials for Replicate capability runtime")
    void shouldReturnMissingCredentialsForReplicateImageGenerationProvider() throws Exception {
        AiPlatformModels.ImageGenerationRequest request = new AiPlatformModels.ImageGenerationRequest(
                "replicate",
                null,
                "Frame cinematografico de um carro futurista.",
                "16:9",
                null,
                1,
                null
        );

        mockMvc.perform(post("/api/v1/images/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("replicate"))
                .andExpect(jsonPath("$.modelCode").value("replicate:black-forest-labs/flux-2-dev"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/edit - should expose missing credentials for BFL image editing runtime")
    void shouldReturnMissingCredentialsForBflImageEditingProvider() throws Exception {
        AiPlatformModels.ImageEditRequest request = new AiPlatformModels.ImageEditRequest(
                "bfl",
                null,
                "Transforme a cena em arte cinematográfica.",
                "https://assets.example.com/input.png",
                null
        );

        mockMvc.perform(post("/api/v1/images/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("bfl"))
                .andExpect(jsonPath("$.modelCode").value("bfl:flux-2-pro"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/images/edit - should expose missing credentials for Replicate image editing runtime")
    void shouldReturnMissingCredentialsForReplicateImageEditingProvider() throws Exception {
        AiPlatformModels.ImageEditRequest request = new AiPlatformModels.ImageEditRequest(
                "replicate",
                null,
                "Transforme a foto em um still cinematografico escuro.",
                "https://assets.example.com/input.png",
                null
        );

        mockMvc.perform(post("/api/v1/images/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("replicate"))
                .andExpect(jsonPath("$.modelCode").value("replicate:black-forest-labs/flux-kontext-dev"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("GET /api/v1/images/jobs/bfl/{jobId} - should expose missing credentials for BFL polling")
    void shouldReturnMissingCredentialsForBflImageJobPolling() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/images/jobs/bfl/req-bfl-123")
                        .queryParam("pollingUrl", "https://api.bfl.ai/v1/get_result?id=req-bfl-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("bfl"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("GET /api/v1/images/jobs/replicate/{jobId} - should expose missing credentials for Replicate polling")
    void shouldReturnMissingCredentialsForReplicateImageJobPolling() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/images/jobs/replicate/pred-123")
                        .queryParam("pollingUrl", "https://api.replicate.com/v1/predictions/pred-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("replicate"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/videos/generate - should expose missing credentials for runway capability runtime")
    void shouldReturnMissingCredentialsForVideoGenerationProvider() throws Exception {
        AiPlatformModels.VideoGenerationRequest request = new AiPlatformModels.VideoGenerationRequest(
                "runway",
                null,
                "Anime este frame com camera lenta.",
                "https://assets.example.com/frame.png",
                5,
                "16:9"
        );

        mockMvc.perform(post("/api/v1/videos/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("runway"))
                .andExpect(jsonPath("$.modelCode").value("runway:gen4.5"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/videos/generate - should expose missing credentials for Replicate video runtime")
    void shouldReturnMissingCredentialsForReplicateVideoGenerationProvider() throws Exception {
        AiPlatformModels.VideoGenerationRequest request = new AiPlatformModels.VideoGenerationRequest(
                "replicate",
                null,
                "Anime o frame com parallax e vento sutil.",
                "https://assets.example.com/frame.png",
                5,
                "16:9"
        );

        mockMvc.perform(post("/api/v1/videos/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("replicate"))
                .andExpect(jsonPath("$.modelCode").value("replicate:xai/grok-imagine-video"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("GET /api/v1/videos/jobs/runway/{jobId} - should expose missing credentials for runway polling")
    void shouldReturnMissingCredentialsForVideoJobPolling() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/videos/jobs/runway/task-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("runway"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("GET /api/v1/videos/jobs/replicate/{jobId} - should expose missing credentials for Replicate video polling")
    void shouldReturnMissingCredentialsForReplicateVideoJobPolling() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/videos/jobs/replicate/pred-video-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("replicate"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/audio/stt - should expose missing credentials for deepgram capability runtime")
    void shouldReturnMissingCredentialsForSpeechToTextProvider() throws Exception {
        AiPlatformModels.SpeechToTextRequest request = new AiPlatformModels.SpeechToTextRequest(
                "deepgram",
                null,
                "https://example.com/audio.wav",
                "pt-BR"
        );

        mockMvc.perform(post("/api/v1/audio/stt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("deepgram"))
                .andExpect(jsonPath("$.modelCode").value("deepgram:nova-3"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/audio/stt - should expose missing credentials for assemblyai capability runtime")
    void shouldReturnMissingCredentialsForAssemblyAiSpeechToTextProvider() throws Exception {
        AiPlatformModels.SpeechToTextRequest request = new AiPlatformModels.SpeechToTextRequest(
                "assemblyai",
                null,
                "https://example.com/audio.wav",
                "pt-BR"
        );

        mockMvc.perform(post("/api/v1/audio/stt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("assemblyai"))
                .andExpect(jsonPath("$.modelCode").value("assemblyai:universal"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/audio/tts - should expose missing credentials for elevenlabs capability runtime")
    void shouldReturnMissingCredentialsForTextToSpeechProvider() throws Exception {
        AiPlatformModels.TextToSpeechRequest request = new AiPlatformModels.TextToSpeechRequest(
                "elevenlabs",
                null,
                "Teste de audio.",
                null,
                "mp3_44100_128"
        );

        mockMvc.perform(post("/api/v1/audio/tts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("elevenlabs"))
                .andExpect(jsonPath("$.modelCode").value("elevenlabs:eleven_multilingual_v2"))
                .andExpect(jsonPath("$.status").value("missing_credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/ocr - should expose missing credentials for mistral OCR capability runtime")
    void shouldReturnMissingCredentialsForOcrProvider() throws Exception {
        AiPlatformModels.OcrRequest request = new AiPlatformModels.OcrRequest(
                "mistral",
                null,
                "https://example.com/receipt.png",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/ocr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode").value("mistral"))
                .andExpect(jsonPath("$.modelCode").value("mistral:mistral-ocr-latest"))
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

    @Test
    @DisplayName("POST /api/v1/web-grounded-chat - should accept explicit research provider metadata")
    void shouldAcceptExplicitResearchProviderMetadata() throws Exception {
        AiPlatformModels.WebGroundedChatRequest request = new AiPlatformModels.WebGroundedChatRequest(
                "openai",
                null,
                "Seja direto.",
                "Quais sao os sinais do mercado de IA?",
                null,
                0.2,
                200,
                java.util.List.of(),
                "req-grounded",
                "quality-first",
                false,
                java.util.List.of("mercado", "grounded"),
                "ws-main",
                "exa",
                3
        );

        mockMvc.perform(post("/api/v1/web-grounded-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("missing_credentials"))
                .andExpect(jsonPath("$.error").value("Credenciais ausentes: EXA_API_KEY"))
                .andExpect(jsonPath("$.attemptChain").isArray());
    }
}
