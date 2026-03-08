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
            String requestId
    ) {
    }

    public record ChatResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String content,
            String error,
            UsageMetadata usage,
            BillingMetadata billing,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
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
            String requestId
    ) {
    }

    public record ResponseResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String outputText,
            String error,
            UsageMetadata usage,
            BillingMetadata billing,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
            boolean streamingSupported
    ) {
    }

    public record EmbeddingRequest(
            @NotBlank String providerCode,
            String modelCode,
            @NotBlank String input
    ) {
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
            @Positive Integer topN
    ) {
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

    public record AiSearchRequest(
            @NotBlank String providerCode,
            @NotBlank String query,
            @Positive Integer limit
    ) {
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
            String requestId
    ) {
    }

    public record WebGroundedChatResponse(
            String providerCode,
            String providerName,
            String modelCode,
            String status,
            String content,
            String error,
            UsageMetadata usage,
            List<CitationMetadata> citations,
            List<GroundingMetadata> grounding,
            boolean fallbackUsed,
            List<String> attemptedProviderCodes,
            boolean streamingSupported
    ) {
    }
}
