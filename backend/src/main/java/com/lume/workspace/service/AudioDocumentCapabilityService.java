package com.lume.workspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AudioDocumentCapabilityService {

    private static final Map<String, String> DEFAULT_STT_MODELS = Map.of(
            "deepgram", "deepgram:nova-3",
            "assemblyai", "assemblyai:universal"
    );

    private static final Map<String, String> DEFAULT_TTS_MODELS = Map.of(
            "elevenlabs", "elevenlabs:eleven_multilingual_v2"
    );

    private static final Map<String, String> DEFAULT_OCR_MODELS = Map.of(
            "mistral", "mistral:mistral-ocr-latest"
    );

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AudioDocumentCapabilityService(
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

    public AiPlatformModels.SpeechToTextResponse speechToText(AiPlatformModels.SpeechToTextRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsSpeechToText(provider)) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    null,
                    null,
                    null,
                    "O provedor selecionado nao suporta speech-to-text no runtime atual."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_STT_MODELS);
        if (request.audioUrl() == null || request.audioUrl().isBlank()) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    null,
                    null,
                    null,
                    "audioUrl e obrigatorio para speech-to-text nesta fase."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    null,
                    null,
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.SpeechToTextResponse response = switch (provider.code()) {
                case "deepgram" -> deepgramSpeechToText(provider, model, request);
                case "assemblyai" -> assemblyAiSpeechToText(provider, model, request);
                default -> new AiPlatformModels.SpeechToTextResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        null,
                        null,
                        null,
                        "O adapter deste provedor ainda nao foi implementado para speech-to-text."
                );
            };
            auditLogService.record("ai_audio_stt", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    null,
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.TextToSpeechResponse textToSpeech(AiPlatformModels.TextToSpeechRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsTextToSpeech(provider)) {
            return new AiPlatformModels.TextToSpeechResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    null,
                    null,
                    "O provedor selecionado nao suporta text-to-speech no runtime atual."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_TTS_MODELS);
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.TextToSpeechResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    null,
                    null,
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.TextToSpeechResponse response = switch (provider.code()) {
                case "elevenlabs" -> elevenLabsTextToSpeech(provider, model, request);
                default -> new AiPlatformModels.TextToSpeechResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        null,
                        null,
                        "O adapter deste provedor ainda nao foi implementado para text-to-speech."
                );
            };
            auditLogService.record("ai_audio_tts", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.TextToSpeechResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    null,
                    providerError.getMessage()
            );
        }
    }

    public AiPlatformModels.OcrResponse ocr(AiPlatformModels.OcrRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_WORKSPACE_READ);

        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!supportsOcr(provider)) {
            return new AiPlatformModels.OcrResponse(
                    provider.code(),
                    provider.name(),
                    request.modelCode(),
                    "unsupported",
                    null,
                    List.of(),
                    "O provedor selecionado nao suporta OCR no runtime atual."
            );
        }

        ModelDefinition model = resolveCapabilityModel(provider, request.modelCode(), DEFAULT_OCR_MODELS);
        if ((request.imageUrl() == null || request.imageUrl().isBlank())
                && (request.documentUrl() == null || request.documentUrl().isBlank())) {
            return new AiPlatformModels.OcrResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    null,
                    List.of(),
                    "Informe imageUrl ou documentUrl para executar OCR."
            );
        }

        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (!missingCredentials.isEmpty()) {
            return new AiPlatformModels.OcrResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "missing_credentials",
                    null,
                    List.of(),
                    "Credenciais ausentes: " + String.join(", ", missingCredentials)
            );
        }

        try {
            AiPlatformModels.OcrResponse response = switch (provider.code()) {
                case "mistral" -> mistralOcr(provider, model, request);
                default -> new AiPlatformModels.OcrResponse(
                        provider.code(),
                        provider.name(),
                        model.code(),
                        "unsupported",
                        null,
                        List.of(),
                        "O adapter deste provedor ainda nao foi implementado para OCR."
                );
            };
            auditLogService.record("ai_document_ocr", provider.code(), response.status(), Map.of(
                    "providerCode", provider.code(),
                    "modelCode", response.modelCode(),
                    "status", response.status()
            ));
            return response;
        } catch (RestClientException providerError) {
            return new AiPlatformModels.OcrResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    List.of(),
                    providerError.getMessage()
            );
        }
    }

    private AiPlatformModels.SpeechToTextResponse deepgramSpeechToText(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.SpeechToTextRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("url", request.audioUrl().trim());

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(providerCatalogService.resolveBaseUrl(provider) + "/listen")
                .queryParam("model", externalModelCode(model.code()))
                .queryParam("smart_format", true);
        if (request.languageCode() != null && !request.languageCode().isBlank()) {
            uriBuilder.queryParam("language", request.languageCode().trim());
        }

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(uriBuilder.build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Token " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode alternative = response.path("results").path("channels").isArray() && !response.path("results").path("channels").isEmpty()
                ? response.path("results").path("channels").get(0).path("alternatives").isArray() && !response.path("results").path("channels").get(0).path("alternatives").isEmpty()
                ? response.path("results").path("channels").get(0).path("alternatives").get(0)
                : NullNode.getInstance()
                : NullNode.getInstance();

        return new AiPlatformModels.SpeechToTextResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                alternative.path("transcript").asText(""),
                alternative.path("confidence").isNumber() ? alternative.path("confidence").asDouble() : null,
                null,
                null
        );
    }

    private AiPlatformModels.SpeechToTextResponse assemblyAiSpeechToText(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.SpeechToTextRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("audio_url", request.audioUrl().trim());
        if (request.languageCode() != null && !request.languageCode().isBlank()) {
            payload.put("language_code", normalizeAssemblyLanguage(request.languageCode()));
        }
        payload.put("speech_model", externalModelCode(model.code()));

        JsonNode submission = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/transcript")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String transcriptId = submission != null ? submission.path("id").asText(null) : null;
        if (transcriptId == null || transcriptId.isBlank()) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    null,
                    null,
                    "O provider nao retornou transcript ID para a transcricao."
            );
        }

        JsonNode transcript = submission;
        for (int attempt = 0; attempt < 8; attempt++) {
            String status = transcript.path("status").asText("");
            if ("completed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                break;
            }
            sleepQuietly(250L);
            transcript = restClientBuilder.build()
                    .get()
                    .uri(providerCatalogService.resolveBaseUrl(provider) + "/transcript/" + transcriptId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", providerCatalogService.credentialValue(provider, "apiKey"))
                    .retrieve()
                    .body(JsonNode.class);
        }

        String status = transcript.path("status").asText("processing").toLowerCase();
        if ("completed".equals(status)) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "completed",
                    transcript.path("text").asText(""),
                    null,
                    null,
                    null
            );
        }
        if ("error".equals(status)) {
            return new AiPlatformModels.SpeechToTextResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    null,
                    null,
                    transcript.path("error").asText("A transcricao falhou no provider AssemblyAI.")
            );
        }
        return new AiPlatformModels.SpeechToTextResponse(
                provider.code(),
                provider.name(),
                model.code(),
                status,
                null,
                null,
                null,
                "A transcricao ainda esta em processamento no provider."
        );
    }

    private AiPlatformModels.TextToSpeechResponse elevenLabsTextToSpeech(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.TextToSpeechRequest request
    ) {
        String voiceId = resolveElevenLabsVoiceId(provider, request.voice());
        if (voiceId == null || voiceId.isBlank()) {
            return new AiPlatformModels.TextToSpeechResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "validation_error",
                    null,
                    null,
                    "Nenhum voice ID foi informado ou encontrado para o provider ElevenLabs."
            );
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("text", request.text());
        payload.put("model_id", externalModelCode(model.code()));

        var uri = UriComponentsBuilder
                .fromHttpUrl(providerCatalogService.resolveBaseUrl(provider) + "/v1/text-to-speech/" + voiceId)
                .queryParam("output_format", request.format() != null && !request.format().isBlank() ? request.format().trim() : "mp3_44100_128")
                .build()
                .toUri();

        byte[] audioBytes = restClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header("xi-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(byte[].class);

        if (audioBytes == null || audioBytes.length == 0) {
            return new AiPlatformModels.TextToSpeechResponse(
                    provider.code(),
                    provider.name(),
                    model.code(),
                    "provider_error",
                    null,
                    null,
                    "O provider retornou audio vazio para a conversao de texto em fala."
            );
        }

        return new AiPlatformModels.TextToSpeechResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                null,
                Base64.getEncoder().encodeToString(audioBytes),
                null
        );
    }

    private AiPlatformModels.OcrResponse mistralOcr(
            ProviderDefinition provider,
            ModelDefinition model,
            AiPlatformModels.OcrRequest request
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", externalModelCode(model.code()));
        payload.put("include_image_base64", false);

        ObjectNode document = payload.putObject("document");
        if (request.documentUrl() != null && !request.documentUrl().isBlank()) {
            document.put("type", "document_url");
            document.put("document_url", request.documentUrl().trim());
        } else {
            document.put("type", "image_url");
            document.put("image_url", request.imageUrl().trim());
        }

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(providerCatalogService.resolveBaseUrl(provider) + "/ocr")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + providerCatalogService.credentialValue(provider, "apiKey"))
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        List<String> blocks = response.path("pages").isArray()
                ? java.util.stream.StreamSupport.stream(response.path("pages").spliterator(), false)
                .map(page -> page.path("markdown").asText(""))
                .filter(markdown -> !markdown.isBlank())
                .toList()
                : List.of();

        String combinedText = blocks.isEmpty()
                ? response.path("document_annotation").asText(response.path("text").asText(""))
                : String.join("\n\n", blocks);

        return new AiPlatformModels.OcrResponse(
                provider.code(),
                provider.name(),
                model.code(),
                "completed",
                combinedText,
                blocks,
                null
        );
    }

    private String resolveElevenLabsVoiceId(ProviderDefinition provider, String requestedVoice) {
        if (requestedVoice != null && !requestedVoice.isBlank()) {
            return requestedVoice.trim();
        }

        JsonNode response = restClientBuilder.build()
                .get()
                .uri(UriComponentsBuilder
                        .fromHttpUrl(providerCatalogService.resolveBaseUrl(provider) + "/v2/voices")
                        .queryParam("page_size", 1)
                        .queryParam("include_total_count", false)
                        .build()
                        .toUri())
                .accept(MediaType.APPLICATION_JSON)
                .header("xi-api-key", providerCatalogService.credentialValue(provider, "apiKey"))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("voices").isArray() || response.path("voices").isEmpty()) {
            return null;
        }
        return response.path("voices").get(0).path("voice_id").asText(null);
    }

    private boolean supportsSpeechToText(ProviderDefinition provider) {
        return provider.capabilities().contains("stt")
                && ("deepgram".equals(provider.code()) || "assemblyai".equals(provider.code()));
    }

    private boolean supportsTextToSpeech(ProviderDefinition provider) {
        return provider.capabilities().contains("tts") && "elevenlabs".equals(provider.code());
    }

    private boolean supportsOcr(ProviderDefinition provider) {
        return provider.capabilities().contains("ocr") && "mistral".equals(provider.code());
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

    private String normalizeAssemblyLanguage(String languageCode) {
        return languageCode.trim().replace('-', '_').toLowerCase();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
