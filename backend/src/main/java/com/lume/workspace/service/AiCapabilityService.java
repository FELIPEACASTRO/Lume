package com.lume.workspace.service;

import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ResearchQueryResponse;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiCapabilityService {

    private final InferenceGatewayService inferenceGatewayService;
    private final ProviderCatalogService providerCatalogService;
    private final ResearchService researchService;
    private final ThreatIntelService threatIntelService;

    public AiCapabilityService(
            InferenceGatewayService inferenceGatewayService,
            ProviderCatalogService providerCatalogService,
            ResearchService researchService,
            ThreatIntelService threatIntelService
    ) {
        this.inferenceGatewayService = inferenceGatewayService;
        this.providerCatalogService = providerCatalogService;
        this.researchService = researchService;
        this.threatIntelService = threatIntelService;
    }

    public AiPlatformModels.ChatResponse chat(AiPlatformModels.ChatRequest request) {
        UnifiedInferenceResponse response = inferenceGatewayService.execute(toUnifiedRequest(
                request.providerCode(),
                request.modelCode(),
                request.systemPrompt(),
                request.prompt(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                request.fallbackProviderCodes(),
                request.requestId(),
                request.freeTierOnly()
        ));
        return new AiPlatformModels.ChatResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.status(),
                response.content(),
                response.error(),
                usage(response),
                billing(response),
                List.of(),
                List.of(),
                response.fallbackUsed(),
                response.attemptedProviderCodes(),
                response.streamingSupported()
        );
    }

    public AiPlatformModels.ResponseResponse responses(AiPlatformModels.ResponseRequest request) {
        UnifiedInferenceResponse response = inferenceGatewayService.execute(toUnifiedRequest(
                request.providerCode(),
                request.modelCode(),
                request.systemPrompt(),
                request.prompt(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                request.fallbackProviderCodes(),
                request.requestId(),
                request.freeTierOnly()
        ));
        return new AiPlatformModels.ResponseResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.status(),
                response.content(),
                response.error(),
                usage(response),
                billing(response),
                List.of(),
                List.of(),
                response.fallbackUsed(),
                response.attemptedProviderCodes(),
                response.streamingSupported()
        );
    }

    public AiPlatformModels.EmbeddingResponse embeddings(AiPlatformModels.EmbeddingRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.EmbeddingResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                List.of(),
                null,
                null,
                "Embeddings ainda nao foram ligados a um adapter capability-aware nesta rodada."
        );
    }

    public AiPlatformModels.RerankResponse rerank(AiPlatformModels.RerankRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.RerankResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                List.of(),
                List.of(),
                null,
                "Rerank ainda nao foi ligado a um adapter capability-aware nesta rodada."
        );
    }

    public AiPlatformModels.ImageGenerationResponse generateImage(AiPlatformModels.ImageGenerationRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.ImageGenerationResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                List.of(),
                List.of(),
                null,
                "Image generation ainda depende dos adapters de media desta proxima tranche."
        );
    }

    public AiPlatformModels.ImageEditResponse editImage(AiPlatformModels.ImageEditRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.ImageEditResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                List.of(),
                List.of(),
                null,
                "Image editing ainda depende dos adapters de media desta proxima tranche."
        );
    }

    public AiPlatformModels.VideoGenerationResponse generateVideo(AiPlatformModels.VideoGenerationRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.VideoGenerationResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                List.of(),
                null,
                "Video generation ainda depende dos adapters assincronos desta proxima tranche."
        );
    }

    public AiPlatformModels.SpeechToTextResponse speechToText(AiPlatformModels.SpeechToTextRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.SpeechToTextResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                null,
                null,
                null,
                "Speech-to-text ainda nao foi ligado a um adapter capability-aware nesta rodada."
        );
    }

    public AiPlatformModels.TextToSpeechResponse textToSpeech(AiPlatformModels.TextToSpeechRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.TextToSpeechResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                null,
                null,
                "Text-to-speech ainda nao foi ligado a um adapter capability-aware nesta rodada."
        );
    }

    public AiPlatformModels.OcrResponse ocr(AiPlatformModels.OcrRequest request) {
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        return new AiPlatformModels.OcrResponse(
                provider.code(),
                provider.name(),
                request.modelCode(),
                "unsupported",
                null,
                List.of(),
                "OCR ainda nao foi ligado a um adapter capability-aware nesta rodada."
        );
    }

    public AiPlatformModels.AiSearchResponse search(AiPlatformModels.AiSearchRequest request) {
        ResearchQueryResponse response = researchService.query(new ResearchQueryRequest(
                request.providerCode(),
                request.query(),
                request.limit()
        ));
        List<AiPlatformModels.AiSearchResultItem> items = response.items().stream()
                .map(item -> new AiPlatformModels.AiSearchResultItem(
                        item.title(),
                        item.url(),
                        item.snippet(),
                        item.source(),
                        item.score()
                ))
                .toList();
        return new AiPlatformModels.AiSearchResponse(
                response.providerCode(),
                response.providerName(),
                response.status(),
                items,
                response.error()
        );
    }

    public AiPlatformModels.WebGroundedChatResponse webGroundedChat(AiPlatformModels.WebGroundedChatRequest request) {
        UnifiedInferenceResponse response = inferenceGatewayService.execute(toUnifiedRequest(
                request.providerCode(),
                request.modelCode(),
                request.systemPrompt(),
                request.prompt(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                request.fallbackProviderCodes(),
                request.requestId(),
                false
        ));
        return new AiPlatformModels.WebGroundedChatResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.status(),
                response.content(),
                response.error(),
                usage(response),
                List.of(),
                List.of(),
                response.fallbackUsed(),
                response.attemptedProviderCodes(),
                response.streamingSupported()
        );
    }

    public ThreatIntelQueryResponse threatIntelSearch(ThreatIntelQueryRequest request) {
        return threatIntelService.query(request);
    }

    private UnifiedInferenceRequest toUnifiedRequest(
            String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            List<com.lume.workspace.dto.UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens,
            List<String> fallbackProviderCodes,
            String requestId,
            Boolean freeTierOnly
    ) {
        if ("openrouter".equalsIgnoreCase(providerCatalogService.normalizeProviderCode(providerCode))
                && Boolean.TRUE.equals(freeTierOnly)
                && modelCode != null
                && !modelCode.endsWith(":free")) {
            throw new IllegalArgumentException("OpenRouter em modo gratuito exige modelCode com sufixo :free.");
        }
        return new UnifiedInferenceRequest(
                providerCode,
                modelCode,
                systemPrompt,
                prompt,
                messages,
                temperature,
                maxTokens,
                fallbackProviderCodes == null ? List.of() : fallbackProviderCodes,
                requestId
        );
    }

    private AiPlatformModels.UsageMetadata usage(UnifiedInferenceResponse response) {
        Integer input = response.estimatedInputTokens();
        Integer output = response.estimatedOutputTokens();
        Integer total = input != null || output != null
                ? (input == null ? 0 : input) + (output == null ? 0 : output)
                : null;
        return new AiPlatformModels.UsageMetadata(
                input,
                output,
                total,
                response.estimatedCostUsd()
        );
    }

    private AiPlatformModels.BillingMetadata billing(UnifiedInferenceResponse response) {
        return new AiPlatformModels.BillingMetadata(
                response.estimatedCostUsd(),
                response.estimatedCostUsd() != null ? "USD" : null,
                "heuristic"
        );
    }
}
