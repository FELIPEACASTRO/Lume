package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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

@DisplayName("MediaCapabilityService - Unit Tests")
class MediaCapabilityServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should generate an image with Ideogram")
    void shouldGenerateImageWithIdeogram() {
        MockEnvironment environment = new MockEnvironment().withProperty("IDEOGRAM_API_KEY", "test-ideogram");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.ideogram.ai/v1/ideogram-v3/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Api-Key", "test-ideogram"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "url": "https://cdn.example.com/generated-1.png"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.generateImage(new AiPlatformModels.ImageGenerationRequest(
                "ideogram",
                null,
                "Paisagem alpina minimalista",
                "1:1",
                "REALISTIC",
                1,
                null
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("ideogram:v3");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/generated-1.png");
    }

    @Test
    @DisplayName("should edit an image with Ideogram using remote image and mask")
    void shouldEditImageWithIdeogram() {
        MockEnvironment environment = new MockEnvironment().withProperty("IDEOGRAM_API_KEY", "test-ideogram");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://assets.example.com/input.png"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("PNGDATA", MediaType.IMAGE_PNG));
        server.expect(requestTo("https://assets.example.com/mask.png"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("MASKDATA", MediaType.IMAGE_PNG));
        server.expect(requestTo("https://api.ideogram.ai/v1/ideogram-v3/edit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Api-Key", "test-ideogram"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {
                              "url": "https://cdn.example.com/edited-1.png"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageEditResponse response = service.editImage(new AiPlatformModels.ImageEditRequest(
                "ideogram",
                null,
                "Substitua o fundo por um estudio branco",
                "https://assets.example.com/input.png",
                "https://assets.example.com/mask.png"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("ideogram:v3");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/edited-1.png");
    }

    @Test
    @DisplayName("should submit a Runway image-to-video job")
    void shouldSubmitRunwayImageToVideoJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("RUNWAY_API_KEY", "test-runway");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dev.runwayml.com/v1/image_to_video"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-runway"))
                .andExpect(header("X-Runway-Version", "2024-11-06"))
                .andRespond(withSuccess("""
                        {
                          "id": "task-123",
                          "status": "PENDING"
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.VideoGenerationResponse response = service.generateVideo(new AiPlatformModels.VideoGenerationRequest(
                "runway",
                null,
                "Adicione movimento cinematografico sutil",
                "https://assets.example.com/frame.png",
                5,
                "16:9"
        ));

        assertThat(response.status()).isEqualTo("submitted");
        assertThat(response.modelCode()).isEqualTo("runway:gen4.5");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().jobId()).isEqualTo("task-123");
        assertThat(response.asyncJob().pollPath()).isEqualTo("/api/v1/videos/jobs/runway/task-123");
    }

    @Test
    @DisplayName("should submit a BFL image generation job")
    void shouldSubmitBflImageGenerationJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("BFL_API_KEY", "test-bfl");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.bfl.ai/v1/flux-2-pro"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-key", "test-bfl"))
                .andRespond(withSuccess("""
                        {
                          "id": "req-bfl-123",
                          "polling_url": "https://api.bfl.ai/v1/get_result?id=req-bfl-123"
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.generateImage(new AiPlatformModels.ImageGenerationRequest(
                "bfl",
                null,
                "Poster editorial com luz dramática",
                "1:1",
                null,
                1,
                null
        ));

        assertThat(response.status()).isEqualTo("submitted");
        assertThat(response.modelCode()).isEqualTo("bfl:flux-2-pro");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().jobId()).isEqualTo("req-bfl-123");
        assertThat(response.asyncJob().pollPath()).contains("/api/v1/images/jobs/bfl/req-bfl-123");
        assertThat(response.asyncJob().pollPath()).contains("pollingUrl=");
    }

    @Test
    @DisplayName("should generate an image with Stability AI")
    void shouldGenerateImageWithStabilityAi() {
        MockEnvironment environment = new MockEnvironment().withProperty("STABILITY_API_KEY", "test-stability");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.stability.ai/v2beta/stable-image/generate/core"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-stability"))
                .andExpect(header("Accept", "application/json"))
                .andRespond(withSuccess("""
                        {
                          "image": "iVBORw0KGgoAAAANSUhEUgAAABAAAA",
                          "finish_reason": "SUCCESS",
                          "seed": 123456
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.generateImage(new AiPlatformModels.ImageGenerationRequest(
                "stability-ai",
                null,
                "Poster editorial com sombra dura",
                "16:9",
                "photographic",
                1,
                "low quality"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("stability-ai:stable-image-core");
        assertThat(response.assetBase64()).containsExactly("iVBORw0KGgoAAAANSUhEUgAAABAAAA");
    }

    @Test
    @DisplayName("should submit a Replicate image generation job")
    void shouldSubmitReplicateImageGenerationJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("REPLICATE_API_TOKEN", "test-replicate");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.replicate.com/v1/models/black-forest-labs/flux-2-dev/predictions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-replicate"))
                .andRespond(withSuccess("""
                        {
                          "id": "pred-123",
                          "status": "starting",
                          "model": "black-forest-labs/flux-2-dev",
                          "urls": {
                            "get": "https://api.replicate.com/v1/predictions/pred-123"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.generateImage(new AiPlatformModels.ImageGenerationRequest(
                "replicate",
                null,
                "Editorial product shot with high contrast lighting",
                "16:9",
                null,
                1,
                null
        ));

        assertThat(response.status()).isEqualTo("submitted");
        assertThat(response.modelCode()).isEqualTo("replicate:black-forest-labs/flux-2-dev");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().jobId()).isEqualTo("pred-123");
        assertThat(response.asyncJob().pollPath()).contains("/api/v1/images/jobs/replicate/pred-123");
        assertThat(response.asyncJob().pollPath()).contains("pollingUrl=");
    }

    @Test
    @DisplayName("should submit a Replicate image edit job")
    void shouldSubmitReplicateImageEditJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("REPLICATE_API_TOKEN", "test-replicate");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.replicate.com/v1/models/black-forest-labs/flux-kontext-dev/predictions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-replicate"))
                .andRespond(withSuccess("""
                        {
                          "id": "pred-edit-123",
                          "status": "starting",
                          "model": "black-forest-labs/flux-kontext-dev",
                          "urls": {
                            "get": "https://api.replicate.com/v1/predictions/pred-edit-123"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageEditResponse response = service.editImage(new AiPlatformModels.ImageEditRequest(
                "replicate",
                null,
                "Turn this scene into a moody cinematic portrait",
                "https://assets.example.com/input.png",
                null
        ));

        assertThat(response.status()).isEqualTo("submitted");
        assertThat(response.modelCode()).isEqualTo("replicate:black-forest-labs/flux-kontext-dev");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().jobId()).isEqualTo("pred-edit-123");
    }

    @Test
    @DisplayName("should poll a Replicate image generation job")
    void shouldPollReplicateImageGenerationJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("REPLICATE_API_TOKEN", "test-replicate");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.replicate.com/v1/predictions/pred-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-replicate"))
                .andRespond(withSuccess("""
                        {
                          "id": "pred-123",
                          "status": "succeeded",
                          "model": "black-forest-labs/flux-2-dev",
                          "output": [
                            "https://cdn.example.com/replicate-image.png"
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.imageJobStatus(
                "replicate",
                "pred-123",
                "https://api.replicate.com/v1/predictions/pred-123"
        );

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("replicate:black-forest-labs/flux-2-dev");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/replicate-image.png");
    }

    @Test
    @DisplayName("should submit a Replicate video generation job")
    void shouldSubmitReplicateVideoGenerationJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("REPLICATE_API_TOKEN", "test-replicate");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.replicate.com/v1/models/xai/grok-imagine-video/predictions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-replicate"))
                .andRespond(withSuccess("""
                        {
                          "id": "pred-video-123",
                          "status": "starting",
                          "model": "xai/grok-imagine-video"
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.VideoGenerationResponse response = service.generateVideo(new AiPlatformModels.VideoGenerationRequest(
                "replicate",
                null,
                "Animate the frame with cinematic camera drift",
                "https://assets.example.com/frame.png",
                5,
                "16:9"
        ));

        assertThat(response.status()).isEqualTo("submitted");
        assertThat(response.modelCode()).isEqualTo("replicate:xai/grok-imagine-video");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().jobId()).isEqualTo("pred-video-123");
        assertThat(response.asyncJob().pollPath()).isEqualTo("/api/v1/videos/jobs/replicate/pred-video-123");
    }

    @Test
    @DisplayName("should poll Replicate video generation job status")
    void shouldPollReplicateVideoGenerationJobStatus() {
        MockEnvironment environment = new MockEnvironment().withProperty("REPLICATE_API_TOKEN", "test-replicate");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.replicate.com/v1/predictions/pred-video-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-replicate"))
                .andRespond(withSuccess("""
                        {
                          "id": "pred-video-123",
                          "status": "succeeded",
                          "model": "xai/grok-imagine-video",
                          "output": "https://cdn.example.com/replicate-video.mp4"
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.VideoGenerationResponse response = service.videoJobStatus("replicate", "pred-video-123");

        assertThat(response.status()).isEqualTo("succeeded");
        assertThat(response.modelCode()).isEqualTo("replicate:xai/grok-imagine-video");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/replicate-video.mp4");
    }

    @Test
    @DisplayName("should poll a BFL image generation job")
    void shouldPollBflImageGenerationJob() {
        MockEnvironment environment = new MockEnvironment().withProperty("BFL_API_KEY", "test-bfl");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.bfl.ai/v1/get_result?id=req-bfl-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-key", "test-bfl"))
                .andRespond(withSuccess("""
                        {
                          "status": "Ready",
                          "result": {
                            "sample": "https://cdn.example.com/flux-2-pro.jpg"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.ImageGenerationResponse response = service.imageJobStatus(
                "bfl",
                "req-bfl-123",
                "https://api.bfl.ai/v1/get_result?id=req-bfl-123"
        );

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("bfl:flux-2-pro");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/flux-2-pro.jpg");
    }

    @Test
    @DisplayName("should poll Runway task status")
    void shouldPollRunwayTaskStatus() {
        MockEnvironment environment = new MockEnvironment().withProperty("RUNWAY_API_KEY", "test-runway");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dev.runwayml.com/v1/tasks/task-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-runway"))
                .andExpect(header("X-Runway-Version", "2024-11-06"))
                .andRespond(withSuccess("""
                        {
                          "id": "task-123",
                          "model": "gen4.5",
                          "status": "SUCCEEDED",
                          "output": [
                            "https://cdn.example.com/video.mp4"
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        MediaCapabilityService service = service(catalogService, builder);
        AiPlatformModels.VideoGenerationResponse response = service.videoJobStatus("runway", "task-123");

        assertThat(response.status()).isEqualTo("succeeded");
        assertThat(response.modelCode()).isEqualTo("runway:gen4.5");
        assertThat(response.assetUrls()).containsExactly("https://cdn.example.com/video.mp4");
        assertThat(response.asyncJob()).isNotNull();
        assertThat(response.asyncJob().status()).isEqualTo("succeeded");
    }

    private MediaCapabilityService service(ProviderCatalogService catalogService, RestClient.Builder builder) {
        return new MediaCapabilityService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                builder,
                objectMapper
        );
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
