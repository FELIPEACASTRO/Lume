package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MediaCapabilityService {

    private static final String RUNWAY_API_VERSION = "2024-11-06";
    private static final Map<String, String> DEFAULT_IMAGE_MODELS = Map.of(
            "ideogram", "ideogram:v3",
            "bfl", "bfl:flux-2-pro",
            "stability-ai", "stability-ai:stable-image-core",
            "replicate", "replicate:black-forest-labs/flux-2-dev",
            "fal-ai", "fal-ai:fal-ai/flux/schnell"
    );
    private static final Map<String, String> DEFAULT_IMAGE_EDIT_MODELS = Map.of(
            "ideogram", "ideogram:v3",
            "bfl", "bfl:flux-2-pro",
            "replicate", "replicate:black-forest-labs/flux-kontext-dev",
            "fal-ai", "fal-ai:fal-ai/flux/schnell"
    );
    private static final Map<String, String> DEFAULT_VIDEO_MODELS = Map.of(
            "runway", "runway:gen4.5",
            "replicate", "replicate:xai/grok-imagine-video",
            "fal-ai", "fal-ai:fal-ai/minimax/hailuo-02/standard/image-to-video"
    );

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public MediaCapabilityService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiPlatformModels.ImageGenerationResponse generateImage(AiPlatformModels.ImageGenerationRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsImageGeneration(provider)) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    List.of(),
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece geracao de imagem nesta integracao."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_IMAGE_MODELS);
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    List.of(),
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.ImageGenerationResponse response = switch (provider.code()) {
                case "ideogram" -> ideogramGenerate(provider, model, request);
                case "bfl" -> bflGenerate(provider, model, request);
                case "stability-ai" -> stabilityGenerate(provider, model, request);
                case "replicate" -> replicateGenerate(provider, model, request);
                case "fal-ai" -> falGenerate(provider, model, request);
                default -> new AiPlatformModels.ImageGenerationResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        List.of(),
                        List.of(),
                        null,
                        "O adapter deste provedor ainda nao foi implementado para image generation."
                );
            };
            auditLogService.record("ai_image_generation", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status(),
                    "assetCount", response.assetUrls().size() + response.assetBase64().size()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    List.of(),
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.ImageEditResponse editImage(AiPlatformModels.ImageEditRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsImageEditing(provider)) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    List.of(),
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece edicao de imagem nesta integracao."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_IMAGE_EDIT_MODELS);
        if (request.inputImageUrl() == null || request.inputImageUrl().isBlank()) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "inputImageUrl e obrigatorio para image editing nesta fase."
            );
        }
        if ("ideogram".equals(provider.code()) && (request.maskImageUrl() == null || request.maskImageUrl().isBlank())) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "maskImageUrl e obrigatorio para image editing nesta fase."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    List.of(),
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.ImageEditResponse response = switch (provider.code()) {
                case "ideogram" -> ideogramEdit(provider, model, request);
                case "bfl" -> bflEdit(provider, model, request);
                case "replicate" -> replicateEdit(provider, model, request);
                case "fal-ai" -> falEdit(provider, model, request);
                default -> new AiPlatformModels.ImageEditResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        List.of(),
                        List.of(),
                        null,
                        "O adapter deste provedor ainda nao foi implementado para image editing."
                );
            };
            auditLogService.record("ai_image_edit", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status(),
                    "assetCount", response.assetUrls().size() + response.assetBase64().size()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    List.of(),
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.VideoGenerationResponse generateVideo(AiPlatformModels.VideoGenerationRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsVideoGeneration(provider)) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece geracao de video nesta integracao."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_VIDEO_MODELS);
        if (request.inputImageUrl() == null || request.inputImageUrl().isBlank()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    null,
                    "inputImageUrl e obrigatorio para o fluxo de image-to-video suportado nesta fase."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.VideoGenerationResponse response = switch (provider.code()) {
                case "runway" -> runwaySubmit(provider, model, request);
                case "replicate" -> replicateVideoSubmit(provider, model, request);
                case "fal-ai" -> falVideoSubmit(provider, model, request);
                default -> new AiPlatformModels.VideoGenerationResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        List.of(),
                        null,
                        "O adapter deste provedor ainda nao foi implementado para video generation."
                );
            };
            auditLogService.record("ai_video_generation", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status(),
                    "jobId", response.asyncJob() != null ? response.asyncJob().jobId() : null
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.VideoGenerationResponse videoJobStatus(String providerCode, String jobId) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        if (!supportsVideoGeneration(provider)) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    null,
                    "unsupported",
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece geracao de video nesta integracao."
            );
        }
        if (jobId == null || jobId.isBlank()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    null,
                    "validation_error",
                    List.of(),
                    null,
                    "jobId e obrigatorio para consultar o status do video."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    "missing_credentials",
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.VideoGenerationResponse response = switch (provider.code()) {
                case "runway" -> runwayTaskStatus(provider, jobId);
                case "replicate" -> replicateVideoJobStatus(provider, jobId);
                case "fal-ai" -> falVideoJobStatus(provider, jobId);
                default -> new AiPlatformModels.VideoGenerationResponse(
                        provider.code(),
                        provider.name(),
                        null,
                        "unsupported",
                        List.of(),
                        null,
                        "O adapter deste provedor ainda nao foi implementado para polling de video."
                );
            };
            auditLogService.record("ai_video_job_status", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "status", response.status(),
                    "jobId", jobId
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    "provider_error",
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.ImageGenerationResponse imageJobStatus(String providerCode, String jobId, String pollingUrl) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        if (!supportsAsyncImagePolling(provider)) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    null,
                    "unsupported",
                    List.of(),
                    List.of(),
                    null,
                    "O provedor selecionado nao oferece acompanhamento de jobs de imagem nesta integracao."
            );
        }
        if (jobId == null || jobId.isBlank()) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    null,
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "jobId e obrigatorio para consultar o status da imagem."
            );
        }
        String effectivePollingUrl = defaultImagePollingUrl(provider, jobId, pollingUrl);
        if (effectivePollingUrl == null || effectivePollingUrl.isBlank()) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "pollingUrl e obrigatorio para consultar jobs do provider nesta fase."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    "missing_credentials",
                    List.of(),
                    List.of(),
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.ImageGenerationResponse response = switch (provider.code()) {
                case "bfl" -> bflJobStatus(provider, jobId, effectivePollingUrl);
                case "replicate" -> replicateImageJobStatus(provider, jobId, effectivePollingUrl);
                case "fal-ai" -> falImageJobStatus(provider, jobId, effectivePollingUrl);
                default -> new AiPlatformModels.ImageGenerationResponse(
                        provider.code(),
                        provider.name(),
                        null,
                        "unsupported",
                        List.of(),
                        List.of(),
                        null,
                        "O adapter deste provedor ainda nao foi implementado para polling de imagem."
                );
            };
            auditLogService.record("ai_image_job_status", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "status", response.status(),
                    "jobId", jobId
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    "provider_error",
                    List.of(),
                    List.of(),
                    null,
                    providerError.getMessage()
            );
        }
    }

    private AiPlatformModels.ImageGenerationResponse ideogramGenerate(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageGenerationRequest request
    ) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("prompt", request.prompt().trim());
        if (request.numberOfImages() != null) {
            form.add("num_images", request.numberOfImages().toString());
        }
        if (request.negativePrompt() != null && !request.negativePrompt().isBlank()) {
            form.add("negative_prompt", request.negativePrompt().trim());
        }
        addIdeogramSizeFields(form, request.size());
        if (request.stylePreset() != null && !request.stylePreset().isBlank()) {
            form.add("style_preset", request.stylePreset().trim());
        }

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/v1/ideogram-v3/generate")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Api-Key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        List<String> urls = extractDataUrls(response);
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                urls,
                List.of(),
                null,
                urls.isEmpty() ? "O provider nao retornou assets para a geracao solicitada." : null
        );
    }

    private AiPlatformModels.ImageEditResponse ideogramEdit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageEditRequest request
    ) {
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("prompt", request.prompt().trim());
        form.add("image", binaryPart(request.inputImageUrl(), "image"));
        form.add("mask", binaryPart(request.maskImageUrl(), "mask"));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/v1/ideogram-v3/edit")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Api-Key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        List<String> urls = extractDataUrls(response);
        return new AiPlatformModels.ImageEditResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                urls,
                List.of(),
                null,
                urls.isEmpty() ? "O provider nao retornou assets para a edicao solicitada." : null
        );
    }

    private AiPlatformModels.ImageGenerationResponse bflGenerate(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageGenerationRequest request
    ) {
        if (request.numberOfImages() != null && request.numberOfImages() > 1) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "A integracao atual com BFL suporta uma imagem por chamada."
            );
        }

        JsonNode response = bflSubmit(provider, model, request.prompt(), request.size(), null);
        return bflSubmissionResponse(provider, model, response, "image generation");
    }

    private AiPlatformModels.ImageGenerationResponse stabilityGenerate(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageGenerationRequest request
    ) {
        if (request.numberOfImages() != null && request.numberOfImages() > 1) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "A integracao atual com Stability AI suporta uma imagem por chamada."
            );
        }

        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("prompt", request.prompt().trim());
        form.add("output_format", "png");
        form.add("aspect_ratio", normalizeStabilityAspectRatio(request.size()));
        if (request.negativePrompt() != null && !request.negativePrompt().isBlank()) {
            form.add("negative_prompt", request.negativePrompt().trim());
        }
        if (request.stylePreset() != null && !request.stylePreset().isBlank()) {
            form.add("style_preset", request.stylePreset().trim().toLowerCase(Locale.ROOT));
        }

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/v2beta/stable-image/generate/core")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        List<String> images = extractStabilityBase64(response);
        String finishReason = response != null ? response.path("finish_reason").asText(null) : null;
        String status = normalizeStabilityStatus(images, finishReason);
        String error = "failed".equals(status)
                ? firstNonBlank(
                response != null ? response.path("message").asText(null) : null,
                response != null ? response.path("errors").path(0).asText(null) : null,
                "A Stability AI bloqueou ou falhou a geracao da imagem."
        )
                : null;
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                status,
                List.of(),
                images,
                null,
                error
        );
    }

    private AiPlatformModels.ImageEditResponse bflEdit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageEditRequest request
    ) {
        JsonNode response = bflSubmit(provider, model, request.prompt(), null, request.inputImageUrl());
        AiPlatformModels.AsyncJobHandle asyncJob = asyncImageJob(provider, response);
        return new AiPlatformModels.ImageEditResponse(
                provider.code(),
                provider.name(),
                model.code(),
                asyncJob == null ? "provider_error" : "submitted",
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? "O provider nao retornou um polling_url valido para image editing." : null
        );
    }

    private AiPlatformModels.ImageGenerationResponse replicateGenerate(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageGenerationRequest request
    ) {
        if (request.numberOfImages() != null && request.numberOfImages() > 1) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    List.of(),
                    List.of(),
                    null,
                    "A integracao atual com Replicate suporta uma imagem por chamada."
            );
        }
        ObjectNode input = objectMapper.createObjectNode();
        input.put("prompt", request.prompt().trim());
        input.put("output_format", "png");
        input.put("aspect_ratio", normalizeReplicateAspectRatio(request.size(), false));
        if (request.negativePrompt() != null && !request.negativePrompt().isBlank()) {
            input.put("negative_prompt", request.negativePrompt().trim());
        }
        return replicateImagePredictionResponse(provider, model, replicatePrediction(provider, model, input), "image generation");
    }

    private AiPlatformModels.ImageEditResponse replicateEdit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageEditRequest request
    ) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("prompt", request.prompt().trim());
        input.put("input_image", request.inputImageUrl().trim());
        input.put("output_format", "png");
        input.put("aspect_ratio", "match_input_image");

        JsonNode response = replicatePrediction(provider, model, input);
        String providerStatus = response != null ? response.path("status").asText(null) : null;
        List<String> urls = extractReplicateOutputUrls(response);
        String modelCode = replicateModelCode(provider, response, model.code());
        if ("completed".equals(normalizeReplicateImageStatus(providerStatus)) && !urls.isEmpty()) {
            return new AiPlatformModels.ImageEditResponse(
                    provider.code(),
                    provider.name(),
                    modelCode,
                    "completed",
                    urls,
                    List.of(),
                    null,
                    null
            );
        }

        AiPlatformModels.AsyncJobHandle asyncJob = asyncReplicateImageJob(provider, response);
        String status = asyncJob == null ? normalizeReplicateImageStatus(providerStatus) : asyncJob.status();
        return new AiPlatformModels.ImageEditResponse(
                provider.code(),
                provider.name(),
                modelCode,
                status,
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? replicateFailureMessage(response) : null
        );
    }

    private JsonNode bflSubmit(
            ProviderDefinition provider,
            ModelDefinition model,
            String prompt,
            String size,
            String inputImageUrl
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", prompt == null ? "" : prompt.trim());
        int[] dimensions = bflDimensions(size);
        payload.put("width", dimensions[0]);
        payload.put("height", dimensions[1]);
        payload.put("output_format", "jpeg");
        if (inputImageUrl != null && !inputImageUrl.isBlank()) {
            payload.put("input_image", inputImageUrl.trim());
        }

        return restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/" + externalModelCode(model.code()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private AiPlatformModels.ImageGenerationResponse bflSubmissionResponse(
            ProviderDefinition provider,
            ModelDefinition model,
            JsonNode response,
            String operationLabel
    ) {
        AiPlatformModels.AsyncJobHandle asyncJob = asyncImageJob(provider, response);
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                asyncJob == null ? "provider_error" : "submitted",
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? "O provider nao retornou um polling_url valido para " + operationLabel + "." : null
        );
    }

    private AiPlatformModels.AsyncJobHandle asyncImageJob(ProviderDefinition provider, JsonNode response) {
        String jobId = response != null ? response.path("id").asText(null) : null;
        String pollingUrl = response != null ? response.path("polling_url").asText(null) : null;
        if (jobId == null || jobId.isBlank() || pollingUrl == null || pollingUrl.isBlank()) {
            return null;
        }
        String pollPath = UriComponentsBuilder.fromPath("/api/v1/images/jobs/{providerCode}/{jobId}")
                .queryParam("pollingUrl", pollingUrl)
                .buildAndExpand(provider.code(), jobId)
                .encode()
                .toUriString();
        return new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                "submitted",
                pollPath
        );
    }

    private AiPlatformModels.ImageGenerationResponse bflJobStatus(
            ProviderDefinition provider,
            String jobId,
            String pollingUrl
    ) {
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(pollingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .header("x-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeBflStatus(response != null ? response.path("status").asText(null) : null);
        List<String> urls = extractBflResultUrls(response);
        String pollPath = UriComponentsBuilder.fromPath("/api/v1/images/jobs/{providerCode}/{jobId}")
                .queryParam("pollingUrl", pollingUrl)
                .buildAndExpand(provider.code(), jobId)
                .encode()
                .toUriString();
        AiPlatformModels.AsyncJobHandle asyncJob = new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                status,
                pollPath
        );
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                provider.defaultModelCode(),
                status,
                urls,
                List.of(),
                asyncJob,
                bflFailureMessage(response)
        );
    }

    private JsonNode replicatePrediction(
            ProviderDefinition provider,
            ModelDefinition model,
            ObjectNode input
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("input", input);

        return restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/models/" + externalModelCode(model.code()) + "/predictions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiToken"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private AiPlatformModels.ImageGenerationResponse replicateImagePredictionResponse(
            ProviderDefinition provider,
            ModelDefinition model,
            JsonNode response,
            String operationLabel
    ) {
        String providerStatus = response != null ? response.path("status").asText(null) : null;
        String status = normalizeReplicateImageStatus(providerStatus);
        List<String> urls = extractReplicateOutputUrls(response);
        String modelCode = replicateModelCode(provider, response, model.code());
        if ("completed".equals(status) && !urls.isEmpty()) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    modelCode,
                    "completed",
                    urls,
                    List.of(),
                    null,
                    null
            );
        }

        AiPlatformModels.AsyncJobHandle asyncJob = asyncReplicateImageJob(provider, response);
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                modelCode,
                asyncJob == null ? status : asyncJob.status(),
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? replicateFailureMessage(response) : "failed".equals(status)
                        ? firstNonBlank(replicateFailureMessage(response), "O provider nao retornou um polling URL valido para " + operationLabel + ".")
                        : null
        );
    }

    private AiPlatformModels.AsyncJobHandle asyncReplicateImageJob(ProviderDefinition provider, JsonNode response) {
        String jobId = response != null ? response.path("id").asText(null) : null;
        if (jobId == null || jobId.isBlank()) {
            return null;
        }
        String pollingUrl = firstNonBlank(
                response != null ? response.path("urls").path("get").asText(null) : null,
                providerCatalogService.resolveBaseUrl(provider) + "/predictions/" + jobId
        );
        String status = normalizeReplicateImageStatus(response != null ? response.path("status").asText(null) : null);
        String pollPath = UriComponentsBuilder.fromPath("/api/v1/images/jobs/{providerCode}/{jobId}")
                .queryParam("pollingUrl", pollingUrl)
                .buildAndExpand(provider.code(), jobId)
                .encode()
                .toUriString();
        return new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                status,
                pollPath
        );
    }

    private AiPlatformModels.ImageGenerationResponse replicateImageJobStatus(
            ProviderDefinition provider,
            String jobId,
            String pollingUrl
    ) {
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(pollingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiToken"))
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeReplicateImageStatus(response != null ? response.path("status").asText(null) : null);
        List<String> urls = extractReplicateOutputUrls(response);
        String pollPath = UriComponentsBuilder.fromPath("/api/v1/images/jobs/{providerCode}/{jobId}")
                .queryParam("pollingUrl", pollingUrl)
                .buildAndExpand(provider.code(), jobId)
                .encode()
                .toUriString();
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                replicateModelCode(provider, response, provider.defaultModelCode()),
                status,
                urls,
                List.of(),
                new AiPlatformModels.AsyncJobHandle(provider.code(), provider.name(), jobId, status, pollPath),
                replicateFailureMessage(response)
        );
    }

    private AiPlatformModels.VideoGenerationResponse runwaySubmit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.VideoGenerationRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("promptImage", request.inputImageUrl().trim());
        if (request.prompt() != null && !request.prompt().isBlank()) {
            payload.put("promptText", request.prompt().trim());
        }
        if (request.durationSeconds() != null && request.durationSeconds() > 0) {
            payload.put("duration", request.durationSeconds());
        }
        payload.put("ratio", normalizeRunwayRatio(request.aspectRatio()));

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/image_to_video")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .header("X-Runway-Version", RUNWAY_API_VERSION)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String jobId = response != null ? response.path("id").asText(null) : null;
        if (jobId == null || jobId.isBlank()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    List.of(),
                    null,
                    "O provider nao retornou um jobId para o video."
            );
        }

        AiPlatformModels.AsyncJobHandle asyncJob = new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                "submitted",
                "/api/v1/videos/jobs/" + provider.code() + "/" + jobId
        );
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "submitted",
                List.of(),
                asyncJob,
                null
        );
    }

    private AiPlatformModels.VideoGenerationResponse runwayTaskStatus(ProviderDefinition provider, String jobId) {
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/tasks/" + jobId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .header("X-Runway-Version", RUNWAY_API_VERSION)
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeRunwayStatus(response != null ? response.path("status").asText(null) : null);
        List<String> urls = extractOutputUrls(response);
        String modelCode = response != null && response.path("model").isTextual()
                ? "runway:" + response.path("model").asText()
                : provider.defaultModelCode();
        AiPlatformModels.AsyncJobHandle asyncJob = new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                status,
                "/api/v1/videos/jobs/" + provider.code() + "/" + jobId
        );
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                modelCode,
                status,
                urls,
                asyncJob,
                runwayFailureMessage(response)
        );
    }

    private AiPlatformModels.VideoGenerationResponse replicateVideoSubmit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.VideoGenerationRequest request
    ) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("prompt", request.prompt().trim());
        input.put("image", request.inputImageUrl().trim());
        input.put("duration", request.durationSeconds() != null && request.durationSeconds() > 0 ? request.durationSeconds() : 5);
        input.put("aspect_ratio", normalizeReplicateVideoAspectRatio(request.aspectRatio()));
        input.put("resolution", "720p");

        JsonNode response = replicatePrediction(provider, model, input);
        String providerStatus = response != null ? response.path("status").asText(null) : null;
        String status = normalizeReplicateVideoStatus(providerStatus);
        List<String> urls = extractReplicateOutputUrls(response);
        String modelCode = replicateModelCode(provider, response, model.code());
        if ("succeeded".equals(status) && !urls.isEmpty()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    modelCode,
                    "succeeded",
                    urls,
                    null,
                    null
            );
        }

        String jobId = response != null ? response.path("id").asText(null) : null;
        if (jobId == null || jobId.isBlank()) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    modelCode,
                    "provider_error",
                    List.of(),
                    null,
                    firstNonBlank(replicateFailureMessage(response), "O provider nao retornou um jobId para o video.")
            );
        }
        AiPlatformModels.AsyncJobHandle asyncJob = new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                jobId,
                status,
                "/api/v1/videos/jobs/" + provider.code() + "/" + jobId
        );
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                modelCode,
                status,
                List.of(),
                asyncJob,
                "failed".equals(status) ? replicateFailureMessage(response) : null
        );
    }

    private AiPlatformModels.VideoGenerationResponse replicateVideoJobStatus(ProviderDefinition provider, String jobId) {
        JsonNode response = restClientBuilder.build()
                .get()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/predictions/" + jobId)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiToken"))
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeReplicateVideoStatus(response != null ? response.path("status").asText(null) : null);
        List<String> urls = extractReplicateOutputUrls(response);
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                replicateModelCode(provider, response, provider.defaultModelCode()),
                status,
                urls,
                new AiPlatformModels.AsyncJobHandle(
                        provider.code(),
                        provider.name(),
                        jobId,
                        status,
                        "/api/v1/videos/jobs/" + provider.code() + "/" + jobId
                ),
                replicateFailureMessage(response)
        );
    }

    private AiPlatformModels.ImageGenerationResponse falGenerate(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageGenerationRequest request
    ) {
        JsonNode response = falSubmit(provider, model, request.prompt(), null, request.size());
        return falSubmissionToImageResponse(provider, model, response);
    }

    private AiPlatformModels.ImageEditResponse falEdit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.ImageEditRequest request
    ) {
        JsonNode response = falSubmit(provider, model, request.prompt(), request.inputImageUrl(), null);
        AiPlatformModels.AsyncJobHandle asyncJob = falAsyncJob(provider, model, response);
        return new AiPlatformModels.ImageEditResponse(
                provider.code(),
                provider.name(),
                model.code(),
                asyncJob == null ? "provider_error" : "submitted",
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? "O provider nao retornou um request_id valido." : null
        );
    }

    private AiPlatformModels.VideoGenerationResponse falVideoSubmit(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.VideoGenerationRequest request
    ) {
        JsonNode response = falSubmit(provider, model, request.prompt(), request.inputImageUrl(), request.aspectRatio());
        AiPlatformModels.AsyncJobHandle asyncJob = falAsyncJob(provider, model, response);
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                asyncJob == null ? "provider_error" : "submitted",
                List.of(),
                asyncJob,
                asyncJob == null ? "O provider nao retornou um request_id valido." : null
        );
    }

    private AiPlatformModels.ImageGenerationResponse falImageJobStatus(
            ProviderDefinition provider,
            String jobId,
            String pollingUrl
    ) {
        JsonNode statusResponse = restClientBuilder.build()
                .get()
                .uri(pollingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Key " + providerCatalogService.credentialValue(provider, "key"))
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeFalStatus(statusResponse.path("status").asText(""));
        if (!"completed".equals(status)) {
            return new AiPlatformModels.ImageGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    status,
                    List.of(),
                    List.of(),
                    falAsyncJob(provider, providerCatalogService.resolveModel(provider.code(), provider.defaultModelCode()), statusResponse),
                    "failed".equals(status) ? firstNonBlank(statusResponse.path("error").asText(null), statusResponse.path("detail").asText(null), "O job de imagem falhou no provider.") : null
            );
        }

        JsonNode result = restClientBuilder.build()
                .get()
                .uri(pollingUrl.replace("/status", ""))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Key " + providerCatalogService.credentialValue(provider, "key"))
                .retrieve()
                .body(JsonNode.class);

        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                provider.defaultModelCode(),
                "completed",
                extractFalUrls(result),
                List.of(),
                null,
                null
        );
    }

    private AiPlatformModels.VideoGenerationResponse falVideoJobStatus(ProviderDefinition provider, String jobId) {
        String pollingUrl = providerCatalogService.resolveBaseUrl(provider) + "/" + externalModelCode(provider.defaultModelCode()) + "/requests/" + jobId + "/status";
        JsonNode statusResponse = restClientBuilder.build()
                .get()
                .uri(pollingUrl)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Key " + providerCatalogService.credentialValue(provider, "key"))
                .retrieve()
                .body(JsonNode.class);

        String status = normalizeFalStatus(statusResponse.path("status").asText(""));
        if (!"completed".equals(status)) {
            return new AiPlatformModels.VideoGenerationResponse(
                    provider.code(),
                    provider.name(),
                    provider.defaultModelCode(),
                    status,
                    List.of(),
                    falAsyncJob(provider, providerCatalogService.resolveModel(provider.code(), provider.defaultModelCode()), statusResponse),
                    "failed".equals(status) ? firstNonBlank(statusResponse.path("error").asText(null), statusResponse.path("detail").asText(null), "O job de video falhou no provider.") : null
            );
        }

        JsonNode result = restClientBuilder.build()
                .get()
                .uri(pollingUrl.replace("/status", ""))
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Key " + providerCatalogService.credentialValue(provider, "key"))
                .retrieve()
                .body(JsonNode.class);

        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                provider.defaultModelCode(),
                "completed",
                extractFalUrls(result),
                null,
                null
        );
    }

    private HttpEntity<ByteArrayResource> binaryPart(String url, String fallbackName) {
        byte[] data = restClientBuilder.build()
                .get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                .retrieve()
                .body(byte[].class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(inferMediaType(url));
        String filename = inferredFilename(url, fallbackName);
        ByteArrayResource resource = new ByteArrayResource(data == null ? new byte[0] : data) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new HttpEntity<>(resource, headers);
    }

    private List<String> extractDataUrls(JsonNode response) {
        if (response == null || !response.path("data").isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(response.path("data").spliterator(), false)
                .map(node -> node.path("url").asText(null))
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private List<String> extractOutputUrls(JsonNode response) {
        if (response == null || !response.path("output").isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(response.path("output").spliterator(), false)
                .map(JsonNode::asText)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private void addIdeogramSizeFields(MultiValueMap<String, Object> form, String size) {
        if (size == null || size.isBlank()) {
            return;
        }
        String normalized = size.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("\\d{3,4}x\\d{3,4}")) {
            form.add("resolution", normalized);
            return;
        }
        if (normalized.matches("\\d{1,2}:\\d{1,2}")) {
            form.add("aspect_ratio", normalized.replace(':', 'x'));
            return;
        }
        if (normalized.matches("\\d{1,2}x\\d{1,2}")) {
            form.add("aspect_ratio", normalized);
        }
    }

    private boolean supportsImageGeneration(ProviderDefinition provider) {
        return provider.capabilities().contains("image") && List.of("ideogram", "bfl", "stability-ai", "replicate", "fal-ai").contains(provider.code());
    }

    private boolean supportsImageEditing(ProviderDefinition provider) {
        return provider.capabilities().contains("image-editing") && List.of("ideogram", "bfl", "replicate", "fal-ai").contains(provider.code());
    }

    private boolean supportsVideoGeneration(ProviderDefinition provider) {
        return provider.capabilities().contains("video") && List.of("runway", "replicate", "fal-ai").contains(provider.code());
    }

    private boolean supportsAsyncImagePolling(ProviderDefinition provider) {
        return List.of("bfl", "replicate", "fal-ai").contains(provider.code());
    }

    private ModelDefinition resolveCapabilityModel(ProviderDefinition provider, String modelCode, Map<String, String> defaults) {
        if (modelCode != null && !modelCode.isBlank()) {
            return providerCatalogService.resolveModel(provider.code(), modelCode);
        }
        return providerCatalogService.resolveModel(provider.code(), defaults.getOrDefault(provider.code(), provider.defaultModelCode()));
    }

    private String externalModelCode(String modelCode) {
        int separatorIndex = modelCode.indexOf(':');
        return separatorIndex >= 0 ? modelCode.substring(separatorIndex + 1) : modelCode;
    }

    private MediaType inferMediaType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String inferredFilename(String url, String fallbackName) {
        String path = URI.create(url).getPath();
        if (path == null || path.isBlank()) {
            return fallbackName;
        }
        int slash = path.lastIndexOf('/');
        String candidate = slash >= 0 ? path.substring(slash + 1) : path;
        if (candidate.isBlank()) {
            return fallbackName;
        }
        return candidate;
    }

    private String normalizeRunwayRatio(String requestedRatio) {
        if (requestedRatio == null || requestedRatio.isBlank()) {
            return "1280:768";
        }
        String normalized = requestedRatio.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "16:9", "1280:720", "1280:768" -> "1280:768";
            case "9:16", "720:1280", "768:1280" -> "768:1280";
            default -> "1280:768";
        };
    }

    private String normalizeReplicateAspectRatio(String requestedRatio, boolean preferInputImage) {
        if (requestedRatio == null || requestedRatio.isBlank()) {
            return preferInputImage ? "match_input_image" : "1:1";
        }
        String normalized = requestedRatio.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1:1", "1024x1024", "1024:1024" -> "1:1";
            case "16:9", "1280x720", "1280:720" -> "16:9";
            case "9:16", "720x1280", "720:1280" -> "9:16";
            case "4:3", "1024x768", "1024:768" -> "4:3";
            case "3:4", "768x1024", "768:1024" -> "3:4";
            case "match_input_image" -> "match_input_image";
            default -> preferInputImage ? "match_input_image" : "1:1";
        };
    }

    private String normalizeReplicateVideoAspectRatio(String requestedRatio) {
        if (requestedRatio == null || requestedRatio.isBlank()) {
            return "auto";
        }
        String normalized = requestedRatio.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1:1", "16:9", "9:16", "4:3", "3:4", "21:9", "auto" -> normalized;
            default -> "auto";
        };
    }

    private String normalizeRunwayStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return "submitted";
        }
        return switch (providerStatus.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCEEDED" -> "succeeded";
            case "FAILED", "CANCELED", "CANCELLED" -> "failed";
            case "EXPIRED" -> "expired";
            case "RUNNING", "PROCESSING" -> "running";
            default -> "submitted";
        };
    }

    private String runwayFailureMessage(JsonNode response) {
        if (response == null) {
            return null;
        }
        String failureCode = response.path("failureCode").asText("");
        String failure = response.path("failure").asText("");
        if (failureCode.isBlank() && failure.isBlank()) {
            return null;
        }
        if (failureCode.isBlank()) {
            return failure;
        }
        if (failure.isBlank()) {
            return failureCode;
        }
        return failureCode + ": " + failure;
    }

    private int[] bflDimensions(String size) {
        if (size == null || size.isBlank()) {
            return new int[]{1024, 1024};
        }
        String normalized = size.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "16:9", "1280x720", "1280:720" -> new int[]{1280, 720};
            case "9:16", "720x1280", "720:1280" -> new int[]{720, 1280};
            case "4:3", "1024x768", "1024:768" -> new int[]{1024, 768};
            case "3:4", "768x1024", "768:1024" -> new int[]{768, 1024};
            default -> new int[]{1024, 1024};
        };
    }

    private String normalizeBflStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return "submitted";
        }
        return switch (providerStatus.trim().toUpperCase(Locale.ROOT)) {
            case "READY" -> "completed";
            case "PENDING", "PROCESSING", "RUNNING" -> "running";
            case "REQUEST MODERATED", "CONTENT MODERATED", "ERROR", "TASK NOT FOUND" -> "failed";
            default -> "submitted";
        };
    }

    private String normalizeReplicateImageStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return "submitted";
        }
        return switch (providerStatus.trim().toLowerCase(Locale.ROOT)) {
            case "succeeded" -> "completed";
            case "failed", "canceled", "cancelled" -> "failed";
            case "processing" -> "running";
            default -> "submitted";
        };
    }

    private String normalizeReplicateVideoStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return "submitted";
        }
        return switch (providerStatus.trim().toLowerCase(Locale.ROOT)) {
            case "succeeded" -> "succeeded";
            case "failed", "canceled", "cancelled" -> "failed";
            case "processing" -> "running";
            default -> "submitted";
        };
    }

    private String normalizeFalStatus(String providerStatus) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return "submitted";
        }
        return switch (providerStatus.trim().toUpperCase(Locale.ROOT)) {
            case "COMPLETED", "SUCCESS" -> "completed";
            case "FAILED", "ERROR" -> "failed";
            case "IN_PROGRESS", "RUNNING", "PROCESSING" -> "running";
            default -> "submitted";
        };
    }

    private List<String> extractReplicateOutputUrls(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        JsonNode output = response.path("output");
        if (output.isTextual()) {
            return List.of(output.asText());
        }
        if (output.isArray()) {
            return java.util.stream.StreamSupport.stream(output.spliterator(), false)
                    .map(node -> node.isTextual() ? node.asText() : node.path("url").asText(null))
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        }
        if (output.path("url").isTextual()) {
            return List.of(output.path("url").asText());
        }
        return List.of();
    }

    private String replicateFailureMessage(JsonNode response) {
        if (response == null) {
            return null;
        }
        return firstNonBlank(
                response.path("error").asText(null),
                response.path("detail").asText(null),
                response.path("message").asText(null)
        );
    }

    private String replicateModelCode(ProviderDefinition provider, JsonNode response, String fallbackModelCode) {
        if (response != null && response.path("model").isTextual() && !response.path("model").asText().isBlank()) {
            return provider.code() + ":" + response.path("model").asText();
        }
        return fallbackModelCode;
    }

    private JsonNode falSubmit(
            ProviderDefinition provider,
            ModelDefinition model,
            String prompt,
            String imageUrl,
            String sizeOrAspectRatio
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("prompt", prompt == null ? "" : prompt.trim());
        if (imageUrl != null && !imageUrl.isBlank()) {
            payload.put("image_url", imageUrl.trim());
        }
        if (sizeOrAspectRatio != null && !sizeOrAspectRatio.isBlank()) {
            payload.put("aspect_ratio", sizeOrAspectRatio.trim());
        }
        return restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/" + externalModelCode(model.code()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Key " + providerCatalogService.credentialValue(provider, "key"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    private AiPlatformModels.AsyncJobHandle falAsyncJob(ProviderDefinition provider, ModelDefinition model, JsonNode response) {
        if (response == null) {
            return null;
        }
        String requestId = firstNonBlank(response.path("request_id").asText(null), response.path("requestId").asText(null));
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        String pollPath = firstNonBlank(
                response.path("status_url").asText(null),
                response.path("statusUrl").asText(null),
                providerCatalogService.resolveBaseUrl(provider) + "/" + externalModelCode(model.code()) + "/requests/" + requestId + "/status"
        );
        return new AiPlatformModels.AsyncJobHandle(
                provider.code(),
                provider.name(),
                requestId,
                normalizeFalStatus(response.path("status").asText("")),
                pollPath
        );
    }

    private AiPlatformModels.ImageGenerationResponse falSubmissionToImageResponse(
            ProviderDefinition provider,
            ModelDefinition model,
            JsonNode response
    ) {
        AiPlatformModels.AsyncJobHandle asyncJob = falAsyncJob(provider, model, response);
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                model.code(),
                asyncJob == null ? "provider_error" : "submitted",
                List.of(),
                List.of(),
                asyncJob,
                asyncJob == null ? "O provider nao retornou um request_id valido." : null
        );
    }

    private List<String> extractFalUrls(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        if (response.path("images").isArray()) {
            return java.util.stream.StreamSupport.stream(response.path("images").spliterator(), false)
                    .map(node -> node.path("url").asText(node.asText(null)))
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        }
        if (response.path("image").path("url").isTextual()) {
            return List.of(response.path("image").path("url").asText());
        }
        if (response.path("video").path("url").isTextual()) {
            return List.of(response.path("video").path("url").asText());
        }
        if (response.path("video").isTextual()) {
            return List.of(response.path("video").asText());
        }
        return List.of();
    }

    private String defaultImagePollingUrl(ProviderDefinition provider, String jobId, String pollingUrl) {
        if (pollingUrl != null && !pollingUrl.isBlank()) {
            return pollingUrl;
        }
        if ("replicate".equals(provider.code())) {
            return providerCatalogService.resolveBaseUrl(provider) + "/predictions/" + jobId;
        }
        if ("fal-ai".equals(provider.code())) {
            return providerCatalogService.resolveBaseUrl(provider) + "/" + externalModelCode(provider.defaultModelCode()) + "/requests/" + jobId + "/status";
        }
        return null;
    }

    private List<String> extractBflResultUrls(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        JsonNode result = response.path("result");
        if (result.path("sample").isTextual()) {
            return List.of(result.path("sample").asText());
        }
        if (result.path("url").isTextual()) {
            return List.of(result.path("url").asText());
        }
        if (result.path("images").isArray()) {
            return java.util.stream.StreamSupport.stream(result.path("images").spliterator(), false)
                    .map(node -> node.path("url").asText(node.asText(null)))
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        }
        return List.of();
    }

    private String bflFailureMessage(JsonNode response) {
        if (response == null) {
            return null;
        }
        String status = response.path("status").asText("");
        if ("READY".equalsIgnoreCase(status)) {
            return null;
        }
        String message = response.path("error").asText("");
        if (message.isBlank()) {
            message = response.path("detail").asText("");
        }
        if (message.isBlank()) {
            message = response.path("message").asText("");
        }
        return message.isBlank() ? null : message;
    }

    private String normalizeStabilityAspectRatio(String size) {
        if (size == null || size.isBlank()) {
            return "1:1";
        }
        String normalized = size.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1:1", "1024x1024", "1024:1024" -> "1:1";
            case "16:9", "1280x720", "1280:720" -> "16:9";
            case "9:16", "720x1280", "720:1280" -> "9:16";
            case "4:3", "1024x768", "1024:768" -> "4:3";
            case "3:4", "768x1024", "768:1024" -> "3:4";
            default -> "1:1";
        };
    }

    private List<String> extractStabilityBase64(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        if (response.path("image").isTextual()) {
            return List.of(response.path("image").asText());
        }
        if (response.path("images").isArray()) {
            return java.util.stream.StreamSupport.stream(response.path("images").spliterator(), false)
                    .map(node -> node.path("base64").asText(node.asText(null)))
                    .filter(image -> image != null && !image.isBlank())
                    .toList();
        }
        if (response.path("artifacts").isArray()) {
            return java.util.stream.StreamSupport.stream(response.path("artifacts").spliterator(), false)
                    .map(node -> node.path("base64").asText(null))
                    .filter(image -> image != null && !image.isBlank())
                    .toList();
        }
        return List.of();
    }

    private String normalizeStabilityStatus(List<String> images, String finishReason) {
        if (!images.isEmpty()) {
            if (finishReason != null && finishReason.equalsIgnoreCase("CONTENT_FILTERED")) {
                return "failed";
            }
            return "completed";
        }
        if (finishReason == null || finishReason.isBlank()) {
            return "provider_error";
        }
        return switch (finishReason.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "COMPLETED" -> "completed";
            case "CONTENT_FILTERED", "ERROR", "FAILED" -> "failed";
            default -> "provider_error";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
