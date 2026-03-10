package com.lume.workspace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class AiPlatformModels {

    private AiPlatformModels() {
    }

    public record UsageMetadata(
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Double estimatedCostUsd
    ) {
    }

    public record BillingMetadata(
            Double estimatedCostUsd,
            String currency,
            String pricingSource
    ) {
    }

    public record CitationMetadata(
            String title,
            String url,
            String snippet
    ) {
    }

    public record GroundingMetadata(
            String providerCode,
            String source,
            Double score
    ) {
    }

    public record AsyncJobHandle(
            String providerCode,
            String providerName,
            String jobId,
            String status,
            String pollPath
    ) {
    }

    public record ExecutionAttemptMetadata(
            String providerCode,
            String status,
            String error,
            Long latencyMs
    ) {
    }

    public record ProviderConfigSnapshot(
            String providerCode,
            String providerName,
            boolean configured,
            boolean executionSupported,
            String catalogState,
            List<String> missingCredentialEnvVars
    ) {
    }

    public record ChatRequest(
            @NotBlank String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            @Valid List<UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens,
            List<String> fallbackProviderCodes,
            Boolean freeTierOnly,
            String requestId,
            String routingMode,
            Boolean stream,
            List<String> tags,
            String workspaceId
    ) {
        public ChatRequest(
                String providerCode,
                String modelCode,
                String systemPrompt,
                String prompt,
                List<UnifiedMessageRequest> messages,
                Double temperature,
                Integer maxTokens,
                List<String> fallbackProviderCodes,
                Boolean freeTierOnly,
                String requestId
        ) {
            this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, fallbackProviderCodes, freeTierOnly, requestId, null, null, List.of(), null);
        }
    }

    public record ChatResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String requestedProviderCode,
            String providerUsed,
            String modelUsed,
            String status,
            String content,
            String error,
            UsageMetadata usage,
            BillingMetadata billing,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
            List<ExecutionAttemptMetadata> attemptChain,
            String streamingMode,
            boolean streamingSupported
    ) {
    }

    public record ResponseRequest(
            @NotBlank String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            @Valid List<UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens,
            List<String> fallbackProviderCodes,
            Boolean freeTierOnly,
            String requestId,
            String routingMode,
            Boolean stream,
            List<String> tags,
            String workspaceId
    ) {
        public ResponseRequest(
                String providerCode,
                String modelCode,
                String systemPrompt,
                String prompt,
                List<UnifiedMessageRequest> messages,
                Double temperature,
                Integer maxTokens,
                List<String> fallbackProviderCodes,
                Boolean freeTierOnly,
                String requestId
        ) {
            this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, fallbackProviderCodes, freeTierOnly, requestId, null, null, List.of(), null);
        }
    }

    public record ResponseResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String requestedProviderCode,
            String providerUsed,
            String modelUsed,
            String status,
            String outputText,
            String error,
            UsageMetadata usage,
            BillingMetadata billing,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
            List<ExecutionAttemptMetadata> attemptChain,
            String streamingMode,
            boolean streamingSupported
    ) {
    }

    public record EmbeddingRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String input,
            String routingMode,
            List<String> tags,
            String workspaceId
    ) {
        public EmbeddingRequest(
                String providerCode,
                String modelCode,
                String input
        ) {
            this(providerCode, modelCode, input, null, List.of(), null);
        }
    }

    public record EmbeddingResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            List<List<Double>> embeddings,
            Integer dimensions,
            UsageMetadata usage,
            String error
    ) {
    }

    public record RerankRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String query,
            List<String> documents,
            @Positive Integer topN,
            String routingMode,
            List<String> tags,
            String workspaceId
    ) {
        public RerankRequest(
                String providerCode,
                String modelCode,
                String query,
                List<String> documents,
                Integer topN
        ) {
            this(providerCode, modelCode, query, documents, topN, null, List.of(), null);
        }
    }

    public record RerankResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            List<Integer> rankedIndexes,
            List<String> rankedDocuments,
            UsageMetadata usage,
            String error
    ) {
    }

    public record ImageGenerationRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String prompt,
            String size,
            String stylePreset,
            Integer numberOfImages,
            String negativePrompt
    ) {
    }

    public record ImageGenerationResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            List<String> assetUrls,
            List<String> assetBase64,
            AsyncJobHandle asyncJob,
            String error
    ) {
    }

    public record ImageEditRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String prompt,
            String inputImageUrl,
            String maskImageUrl
    ) {
    }

    public record ImageEditResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            List<String> assetUrls,
            List<String> assetBase64,
            AsyncJobHandle asyncJob,
            String error
    ) {
    }

    public record VideoGenerationRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String prompt,
            String inputImageUrl,
            Integer durationSeconds,
            String aspectRatio
    ) {
    }

    public record VideoGenerationResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            List<String> assetUrls,
            AsyncJobHandle asyncJob,
            String error
    ) {
    }

    public record SpeechToTextRequest(
            @NotBlank String providerCode,
            String modelCode,
            String audioUrl,
            String languageCode
    ) {
    }

    public record SpeechToTextResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String transcript,
            Double confidence,
            UsageMetadata usage,
            String error
    ) {
    }

    public record TextToSpeechRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String text,
            String voice,
            String format
    ) {
    }

    public record TextToSpeechResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String audioUrl,
            String audioBase64,
            String error
    ) {
    }

    public record OcrRequest(
            @NotBlank String providerCode,
            String modelCode,
            String imageUrl,
            String documentUrl,
            String languageCode
    ) {
    }

    public record OcrResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String text,
            List<String> blocks,
            String error
    ) {
    }

    public record TranslationRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String text,
            @NotBlank String targetLanguageCode,
            String sourceLanguageCode
    ) {
    }

    public record TranslationResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String translatedText,
            String detectedLanguageCode,
            String error
    ) {
    }

    public record NlpEntity(
            String name,
            String type,
            Double salience
    ) {
    }

    public record NlpCategory(
            String name,
            Double confidence
    ) {
    }

    public record NlpAnalysisRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String text,
            @NotBlank String analysisType,
            String languageCode
    ) {
    }

    public record NlpAnalysisResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String languageCode,
            Double sentimentScore,
            Double sentimentMagnitude,
            List<NlpEntity> entities,
            List<NlpCategory> categories,
            String error
    ) {
    }

    public record AiSearchRequest(
            @NotBlank String providerCode,
            @NotBlank String query,
            @Positive Integer limit,
            String routingMode,
            List<String> tags,
            String workspaceId
    ) {
        public AiSearchRequest(
                String providerCode,
                String query,
                Integer limit
        ) {
            this(providerCode, query, limit, null, List.of(), null);
        }
    }

    public record AiSearchResultItem(
            String title,
            String url,
            String snippet,
            String source,
            Double score
    ) {
    }

    public record AiSearchResponse(
            String providerCode,
            String providerName,
            String status,
            List<AiSearchResultItem> items,
            String error
    ) {
    }

    public record WebGroundedChatRequest(
            @NotBlank String providerCode,
            String modelCode,
            String systemPrompt,
            String prompt,
            @Valid List<UnifiedMessageRequest> messages,
            Double temperature,
            Integer maxTokens,
            List<String> fallbackProviderCodes,
            String requestId,
            String routingMode,
            Boolean stream,
            List<String> tags,
            String workspaceId,
            String researchProviderCode,
            Integer searchLimit
    ) {
        public WebGroundedChatRequest(
                String providerCode,
                String modelCode,
                String systemPrompt,
                String prompt,
                List<UnifiedMessageRequest> messages,
                Double temperature,
                Integer maxTokens,
                List<String> fallbackProviderCodes,
                String requestId
        ) {
            this(providerCode, modelCode, systemPrompt, prompt, messages, temperature, maxTokens, fallbackProviderCodes, requestId, null, null, List.of(), null, null, null);
        }
    }

    public record WebGroundedChatResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String requestedProviderCode,
            String providerUsed,
            String modelUsed,
            String status,
            String content,
            String error,
            UsageMetadata usage,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
            List<ExecutionAttemptMetadata> attemptChain,
            String streamingMode,
            boolean streamingSupported
    ) {
    }
}
