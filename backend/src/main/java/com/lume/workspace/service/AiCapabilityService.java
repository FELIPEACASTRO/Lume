package com.lume.workspace.service;

import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ResearchQueryResponse;
import com.lume.workspace.dto.ResearchResultItemResponse;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.orchestration.AiExecutionAttempt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiCapabilityService {

    private final InferenceGatewayService inferenceGatewayService;
    private final ProviderCatalogService providerCatalogService;
    private final VectorCapabilityService vectorCapabilityService;
    private final AudioDocumentCapabilityService audioDocumentCapabilityService;
    private final MediaCapabilityService mediaCapabilityService;
    private final ResearchService researchService;
    private final ThreatIntelService threatIntelService;

    public AiCapabilityService(
            InferenceGatewayService inferenceGatewayService,
            ProviderCatalogService providerCatalogService,
            VectorCapabilityService vectorCapabilityService,
            AudioDocumentCapabilityService audioDocumentCapabilityService,
            MediaCapabilityService mediaCapabilityService,
            ResearchService researchService,
            ThreatIntelService threatIntelService
    ) {
        this.inferenceGatewayService = inferenceGatewayService;
        this.providerCatalogService = providerCatalogService;
        this.vectorCapabilityService = vectorCapabilityService;
        this.audioDocumentCapabilityService = audioDocumentCapabilityService;
        this.mediaCapabilityService = mediaCapabilityService;
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
                request.freeTierOnly(),
                request.routingMode(),
                request.stream(),
                request.tags(),
                request.workspaceId()
        ));
        return new AiPlatformModels.ChatResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.requestedProviderCode(),
                response.providerCode(),
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
                attemptChain(response),
                response.streamingMode(),
                "native".equalsIgnoreCase(response.streamingMode())
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
                request.freeTierOnly(),
                request.routingMode(),
                request.stream(),
                request.tags(),
                request.workspaceId()
        ));
        return new AiPlatformModels.ResponseResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.requestedProviderCode(),
                response.providerCode(),
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
                attemptChain(response),
                response.streamingMode(),
                "native".equalsIgnoreCase(response.streamingMode())
        );
    }

    public AiPlatformModels.EmbeddingResponse embeddings(AiPlatformModels.EmbeddingRequest request) {
        return vectorCapabilityService.embeddings(request);
    }

    public AiPlatformModels.RerankResponse rerank(AiPlatformModels.RerankRequest request) {
        return vectorCapabilityService.rerank(request);
    }

    public AiPlatformModels.ImageGenerationResponse generateImage(AiPlatformModels.ImageGenerationRequest request) {
        return mediaCapabilityService.generateImage(request);
    }

    public AiPlatformModels.ImageEditResponse editImage(AiPlatformModels.ImageEditRequest request) {
        return mediaCapabilityService.editImage(request);
    }

    public AiPlatformModels.ImageGenerationResponse imageJobStatus(String providerCode, String jobId, String pollingUrl) {
        return mediaCapabilityService.imageJobStatus(providerCode, jobId, pollingUrl);
    }

    public AiPlatformModels.VideoGenerationResponse generateVideo(AiPlatformModels.VideoGenerationRequest request) {
        return mediaCapabilityService.generateVideo(request);
    }

    public AiPlatformModels.VideoGenerationResponse videoJobStatus(String providerCode, String jobId) {
        return mediaCapabilityService.videoJobStatus(providerCode, jobId);
    }

    public AiPlatformModels.SpeechToTextResponse speechToText(AiPlatformModels.SpeechToTextRequest request) {
        return audioDocumentCapabilityService.speechToText(request);
    }

    public AiPlatformModels.TextToSpeechResponse textToSpeech(AiPlatformModels.TextToSpeechRequest request) {
        return audioDocumentCapabilityService.textToSpeech(request);
    }

    public AiPlatformModels.OcrResponse ocr(AiPlatformModels.OcrRequest request) {
        return audioDocumentCapabilityService.ocr(request);
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
        ResearchQueryResponse groundingResponse = null;
        String prompt = request.prompt();
        List<AiPlatformModels.CitationMetadata> citations = List.of();
        List<AiPlatformModels.GroundingMetadata> grounding = List.of();

        if (request.researchProviderCode() != null && !request.researchProviderCode().isBlank()) {
            groundingResponse = researchService.query(new ResearchQueryRequest(
                    request.researchProviderCode(),
                    queryForGrounding(request),
                    request.searchLimit() != null ? request.searchLimit() : 5
            ));
            if (!"completed".equalsIgnoreCase(groundingResponse.status())) {
                return new AiPlatformModels.WebGroundedChatResponse(
                        request.providerCode(),
                        providerCatalogService.requireProvider(request.providerCode()).name(),
                        request.modelCode(),
                        request.providerCode(),
                        request.providerCode(),
                        request.modelCode(),
                        groundingResponse.status(),
                        null,
                        groundingResponse.error(),
                        null,
                        List.of(),
                        List.of(),
                        false,
                        List.of(),
                        List.of(),
                        "unsupported",
                        false
                );
            }
            citations = citationsFromResearch(groundingResponse);
            grounding = groundingFromResearch(groundingResponse);
            prompt = groundedPrompt(request.prompt(), groundingResponse);
        }

        UnifiedInferenceResponse response = inferenceGatewayService.execute(toUnifiedRequest(
                request.providerCode(),
                request.modelCode(),
                groundedSystemPrompt(request.systemPrompt(), groundingResponse),
                prompt,
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                request.fallbackProviderCodes(),
                request.requestId(),
                false,
                request.routingMode(),
                request.stream(),
                request.tags(),
                request.workspaceId()
        ));
        return new AiPlatformModels.WebGroundedChatResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.requestedProviderCode(),
                response.providerCode(),
                response.modelCode(),
                response.status(),
                response.content(),
                response.error(),
                usage(response),
                citations,
                grounding,
                response.fallbackUsed(),
                response.attemptedProviderCodes(),
                attemptChain(response),
                response.streamingMode(),
                "native".equalsIgnoreCase(response.streamingMode())
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
            Boolean freeTierOnly,
            String routingMode,
            Boolean stream,
            List<String> tags,
            String workspaceId
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
                requestId,
                routingMode,
                stream,
                tags == null ? List.of() : List.copyOf(tags),
                workspaceId
        );
    }

    private List<AiPlatformModels.ExecutionAttemptMetadata> attemptChain(UnifiedInferenceResponse response) {
        if (response.attemptChain() == null) {
            return List.of();
        }
        return response.attemptChain().stream()
                .map(this::toAttemptMetadata)
                .toList();
    }

    private AiPlatformModels.ExecutionAttemptMetadata toAttemptMetadata(AiExecutionAttempt attempt) {
        return new AiPlatformModels.ExecutionAttemptMetadata(
                attempt.providerCode(),
                attempt.status(),
                attempt.error(),
                attempt.latencyMs()
        );
    }

    private String queryForGrounding(AiPlatformModels.WebGroundedChatRequest request) {
        if (request.prompt() != null && !request.prompt().isBlank()) {
            return request.prompt();
        }
        if (request.messages() != null && !request.messages().isEmpty()) {
            return request.messages().stream()
                    .filter(message -> message != null && "user".equalsIgnoreCase(message.role()))
                    .map(com.lume.workspace.dto.UnifiedMessageRequest::content)
                    .filter(content -> content != null && !content.isBlank())
                    .reduce((first, second) -> second)
                    .orElse("Pesquisa web para resposta grounded.");
        }
        return "Pesquisa web para resposta grounded.";
    }

    private String groundedSystemPrompt(String systemPrompt, ResearchQueryResponse researchResponse) {
        if (researchResponse == null || researchResponse.items().isEmpty()) {
            return systemPrompt;
        }
        String groundingClause = "Use apenas o contexto web verificado fornecido como grounding e cite as fontes relevantes na resposta.";
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return groundingClause;
        }
        return systemPrompt.trim() + "\n\n" + groundingClause;
    }

    private String groundedPrompt(String prompt, ResearchQueryResponse researchResponse) {
        if (researchResponse == null || researchResponse.items().isEmpty()) {
            return prompt;
        }
        List<String> lines = new ArrayList<>();
        lines.add(prompt == null || prompt.isBlank() ? "Responda com base nas fontes abaixo." : prompt.trim());
        lines.add("");
        lines.add("Contexto web verificado:");
        int index = 1;
        for (ResearchResultItemResponse item : researchResponse.items()) {
            lines.add(index + ". " + item.title());
            lines.add("   URL: " + item.url());
            lines.add("   Resumo: " + item.snippet());
            index++;
        }
        return String.join("\n", lines);
    }

    private List<AiPlatformModels.CitationMetadata> citationsFromResearch(ResearchQueryResponse researchResponse) {
        if (researchResponse == null) {
            return List.of();
        }
        return researchResponse.items().stream()
                .map(item -> new AiPlatformModels.CitationMetadata(
                        item.title(),
                        item.url(),
                        item.snippet()
                ))
                .toList();
    }

    private List<AiPlatformModels.GroundingMetadata> groundingFromResearch(ResearchQueryResponse researchResponse) {
        if (researchResponse == null) {
            return List.of();
        }
        return researchResponse.items().stream()
                .map(item -> new AiPlatformModels.GroundingMetadata(
                        researchResponse.providerCode(),
                        item.source(),
                        item.score()
                ))
                .toList();
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
