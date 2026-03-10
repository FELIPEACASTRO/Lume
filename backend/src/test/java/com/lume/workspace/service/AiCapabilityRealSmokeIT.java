package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("real-ai")
@DisplayName("AI Capability Real Smoke - Optional Integration Tests")
class AiCapabilityRealSmokeIT {

    @Test
    @DisplayName("should execute minimal real vector and research calls only when explicitly enabled")
    void shouldExecuteMinimalRealVectorAndResearchCallsOnlyWhenExplicitlyEnabled() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv().getOrDefault("RUN_REAL_AI_TESTS", "false")));

        MockEnvironment environment = new MockEnvironment();
        Map.ofEntries(
                Map.entry("COHERE_API_KEY", System.getenv("COHERE_API_KEY")),
                Map.entry("VOYAGE_API_KEY", System.getenv("VOYAGE_API_KEY")),
                Map.entry("DASHSCOPE_API_KEY", System.getenv("DASHSCOPE_API_KEY")),
                Map.entry("SILICONFLOW_API_KEY", System.getenv("SILICONFLOW_API_KEY")),
                Map.entry("EXA_API_KEY", System.getenv("EXA_API_KEY")),
                Map.entry("NEWSCATCHER_API_KEY", System.getenv("NEWSCATCHER_API_KEY")),
                Map.entry("TAVILY_API_KEY", System.getenv("TAVILY_API_KEY")),
                Map.entry("SERPAPI_API_KEY", System.getenv("SERPAPI_API_KEY")),
                Map.entry("STABILITY_API_KEY", System.getenv("STABILITY_API_KEY")),
                Map.entry("REPLICATE_API_TOKEN", System.getenv("REPLICATE_API_TOKEN")),
                Map.entry("ASSEMBLYAI_API_KEY", System.getenv("ASSEMBLYAI_API_KEY")),
                Map.entry("BFL_API_KEY", System.getenv("BFL_API_KEY"))
        ).forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                environment.setProperty(key, value);
            }
        });

        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();

        VectorCapabilityService vectorCapabilityService = new VectorCapabilityService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                builder,
                new ObjectMapper()
        );
        AudioDocumentCapabilityService audioDocumentCapabilityService = new AudioDocumentCapabilityService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                new GoogleCloudSupportService(catalogService, new ObjectMapper()),
                builder,
                new ObjectMapper()
        );
        MediaCapabilityService mediaCapabilityService = new MediaCapabilityService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                builder,
                new ObjectMapper()
        );
        ResearchService researchService = new ResearchService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                new NoOpWorkspaceLedgerService(),
                builder,
                new ObjectMapper()
        );

        for (String providerCode : List.of("cohere", "voyage-ai", "dashscope-qwen", "siliconflow")) {
            if (!catalogService.isConfigured(providerCode)) {
                continue;
            }
            var response = vectorCapabilityService.embeddings(new AiPlatformModels.EmbeddingRequest(
                providerCode,
                null,
                "Lume workspace overview"
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.embeddings()).isNotEmpty();
        }

        for (String providerCode : List.of("dashscope-qwen", "siliconflow")) {
            if (!catalogService.isConfigured(providerCode)) {
                continue;
            }
            var response = vectorCapabilityService.rerank(new AiPlatformModels.RerankRequest(
                    providerCode,
                    null,
                    "qual documento fala de custo?",
                    List.of("plano de produto", "orcamento e custos"),
                    2
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.rankedIndexes()).isNotEmpty();
        }

        for (String providerCode : List.of("exa", "newscatcher", "tavily", "serpapi")) {
            if (!catalogService.isConfigured(providerCode)) {
                continue;
            }
            var response = researchService.query(new ResearchQueryRequest(providerCode, "Lume AI", 1));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.items()).isNotEmpty();
        }

        if (catalogService.isConfigured("deepgram")) {
            var response = audioDocumentCapabilityService.speechToText(new AiPlatformModels.SpeechToTextRequest(
                    "deepgram",
                    null,
                    System.getenv().getOrDefault("DEEPGRAM_TEST_AUDIO_URL", "https://dpgr.am/spacewalk.wav"),
                    "en"
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.transcript()).isNotBlank();
        }

        if (catalogService.isConfigured("assemblyai")) {
            var response = audioDocumentCapabilityService.speechToText(new AiPlatformModels.SpeechToTextRequest(
                    "assemblyai",
                    null,
                    System.getenv().getOrDefault("ASSEMBLYAI_TEST_AUDIO_URL", "https://assembly.ai/wildfires.mp3"),
                    "en"
            ));
            assertThat(response.status()).isIn("completed", "processing", "queued");
            if ("completed".equals(response.status())) {
                assertThat(response.transcript()).isNotBlank();
            }
        }

        if (catalogService.isConfigured("elevenlabs")) {
            var response = audioDocumentCapabilityService.textToSpeech(new AiPlatformModels.TextToSpeechRequest(
                    "elevenlabs",
                    null,
                    "ping",
                    null,
                    "mp3_44100_128"
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.audioBase64()).isNotBlank();
        }

        if (catalogService.isConfigured("mistral") && System.getenv("MISTRAL_OCR_DOCUMENT_URL") != null && !System.getenv("MISTRAL_OCR_DOCUMENT_URL").isBlank()) {
            var response = audioDocumentCapabilityService.ocr(new AiPlatformModels.OcrRequest(
                    "mistral",
                    null,
                    null,
                    System.getenv("MISTRAL_OCR_DOCUMENT_URL"),
                    null
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.text()).isNotBlank();
        }

        if (catalogService.isConfigured("ideogram")) {
            var response = mediaCapabilityService.generateImage(new AiPlatformModels.ImageGenerationRequest(
                    "ideogram",
                    null,
                    "Poster minimalista com gradiente azul e laranja",
                    "1:1",
                    null,
                    1,
                    null
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.assetUrls()).isNotEmpty();
        }

        if (catalogService.isConfigured("stability-ai")) {
            var response = mediaCapabilityService.generateImage(new AiPlatformModels.ImageGenerationRequest(
                    "stability-ai",
                    null,
                    "Editorial product shot with premium studio lighting",
                    "1:1",
                    null,
                    1,
                    null
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.assetBase64()).isNotEmpty();
        }

        if (catalogService.isConfigured("replicate")) {
            var response = mediaCapabilityService.generateImage(new AiPlatformModels.ImageGenerationRequest(
                    "replicate",
                    null,
                    "Editorial product shot with hard studio shadows",
                    "1:1",
                    null,
                    1,
                    null
            ));
            assertThat(response.status()).isIn("submitted", "running", "completed");
            if (response.asyncJob() != null) {
                String pollingUrl = response.asyncJob().pollPath().contains("pollingUrl=")
                        ? java.net.URLDecoder.decode(response.asyncJob().pollPath().split("pollingUrl=", 2)[1], java.nio.charset.StandardCharsets.UTF_8)
                        : null;
                var status = mediaCapabilityService.imageJobStatus("replicate", response.asyncJob().jobId(), pollingUrl);
                assertThat(status.status()).isNotBlank();
            }
        }

        if (catalogService.isConfigured("ideogram")
                && System.getenv("IDEOGRAM_EDIT_IMAGE_URL") != null && !System.getenv("IDEOGRAM_EDIT_IMAGE_URL").isBlank()
                && System.getenv("IDEOGRAM_EDIT_MASK_URL") != null && !System.getenv("IDEOGRAM_EDIT_MASK_URL").isBlank()) {
            var response = mediaCapabilityService.editImage(new AiPlatformModels.ImageEditRequest(
                    "ideogram",
                    null,
                    "Substitua o fundo por um estudio cinematografico escuro",
                    System.getenv("IDEOGRAM_EDIT_IMAGE_URL"),
                    System.getenv("IDEOGRAM_EDIT_MASK_URL")
            ));
            assertThat(response.status()).isEqualTo("completed");
            assertThat(response.assetUrls()).isNotEmpty();
        }

        if (catalogService.isConfigured("bfl")) {
            var submit = mediaCapabilityService.generateImage(new AiPlatformModels.ImageGenerationRequest(
                    "bfl",
                    null,
                    "Editorial poster with dramatic contrast and clean geometry",
                    "1:1",
                    null,
                    1,
                    null
            ));
            assertThat(submit.status()).isEqualTo("submitted");
            assertThat(submit.asyncJob()).isNotNull();

            String pollingUrl = submit.asyncJob().pollPath().contains("pollingUrl=")
                    ? java.net.URLDecoder.decode(submit.asyncJob().pollPath().split("pollingUrl=", 2)[1], java.nio.charset.StandardCharsets.UTF_8)
                    : null;
            var status = mediaCapabilityService.imageJobStatus("bfl", submit.asyncJob().jobId(), pollingUrl);
            assertThat(status.status()).isNotBlank();
        }

        if (catalogService.isConfigured("runway")
                && System.getenv("RUNWAY_TEST_IMAGE_URL") != null && !System.getenv("RUNWAY_TEST_IMAGE_URL").isBlank()) {
            var submit = mediaCapabilityService.generateVideo(new AiPlatformModels.VideoGenerationRequest(
                    "runway",
                    null,
                    "Adicione movimento cinematografico sutil e parallax",
                    System.getenv("RUNWAY_TEST_IMAGE_URL"),
                    5,
                    "16:9"
            ));
            assertThat(submit.status()).isEqualTo("submitted");
            assertThat(submit.asyncJob()).isNotNull();

            var status = mediaCapabilityService.videoJobStatus("runway", submit.asyncJob().jobId());
            assertThat(status.status()).isNotBlank();
        }

        if (catalogService.isConfigured("replicate")
                && System.getenv("REPLICATE_VIDEO_TEST_IMAGE_URL") != null && !System.getenv("REPLICATE_VIDEO_TEST_IMAGE_URL").isBlank()) {
            var submit = mediaCapabilityService.generateVideo(new AiPlatformModels.VideoGenerationRequest(
                    "replicate",
                    null,
                    "Add cinematic motion and a soft wind pass",
                    System.getenv("REPLICATE_VIDEO_TEST_IMAGE_URL"),
                    5,
                    "16:9"
            ));
            assertThat(submit.status()).isIn("submitted", "running", "succeeded");
            if (submit.asyncJob() != null) {
                var status = mediaCapabilityService.videoJobStatus("replicate", submit.asyncJob().jobId());
                assertThat(status.status()).isNotBlank();
            }
        }
    }

    private static final class TestWorkspaceContextService extends WorkspaceContextService {

        TestWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public jakarta.servlet.http.HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getObject() {
                    return null;
                }
            });
        }

        @Override
        public void requirePermission(String permission) {
        }

        @Override
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public Long getWorkspaceId() {
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }
    }

    private static final class NoOpAuditLogService extends AuditLogService {

        NoOpAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }
    }
}
