package com.lume.workspace.service;

import com.lume.workspace.dto.ProviderConnectivityResponse;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.catalog.ProviderGovernanceMetadata;
import com.lume.workspace.inference.orchestration.ProviderConnectivitySnapshot;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderConnectivityService {

    private final ProviderCatalogService providerCatalogService;
    private final WorkspaceContextService workspaceContextService;
    private final InferenceGatewayService inferenceGatewayService;
    private final VectorCapabilityService vectorCapabilityService;
    private final AudioDocumentCapabilityService audioDocumentCapabilityService;
    private final MediaCapabilityService mediaCapabilityService;
    private final ResearchService researchService;
    private final ThreatIntelService threatIntelService;
    private final Map<String, ProviderConnectivitySnapshot> snapshots;

    public ProviderConnectivityService(
            ProviderCatalogService providerCatalogService,
            WorkspaceContextService workspaceContextService,
            InferenceGatewayService inferenceGatewayService,
            VectorCapabilityService vectorCapabilityService,
            AudioDocumentCapabilityService audioDocumentCapabilityService,
            MediaCapabilityService mediaCapabilityService,
            ResearchService researchService,
            ThreatIntelService threatIntelService
    ) {
        this.providerCatalogService = providerCatalogService;
        this.workspaceContextService = workspaceContextService;
        this.inferenceGatewayService = inferenceGatewayService;
        this.vectorCapabilityService = vectorCapabilityService;
        this.audioDocumentCapabilityService = audioDocumentCapabilityService;
        this.mediaCapabilityService = mediaCapabilityService;
        this.researchService = researchService;
        this.threatIntelService = threatIntelService;
        this.snapshots = new ConcurrentHashMap<>();
    }

    public ProviderConnectivityResponse test(String providerCode) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_PROVIDERS_TEST);

        ProviderDefinition provider = providerCatalogService.requireProvider(providerCode);
        ProviderGovernanceMetadata metadata = providerCatalogService.governanceMetadata(provider);
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);

        if (!missingCredentials.isEmpty()) {
            ProviderConnectivityResponse response = new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "missing_credentials",
                    false,
                    provider.executionSupported(),
                    metadata.implementationStatus(),
                    metadata.evidenceLevel(),
                    providerCatalogService.streamingMode(provider),
                    providerCatalogService.runtimeMaturity(provider),
                    null,
                    "Credenciais ausentes para o connectivity test.",
                    missingCredentials
            );
            remember(response);
            return response;
        }

        if (!provider.executionSupported()) {
            ProviderConnectivityResponse response = new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "manual_setup_required",
                    providerCatalogService.isConfigured(provider),
                    false,
                    metadata.implementationStatus(),
                    metadata.evidenceLevel(),
                    providerCatalogService.streamingMode(provider),
                    providerCatalogService.runtimeMaturity(provider),
                    null,
                    "Esta integracao ainda depende de habilitacao manual antes do uso operacional.",
                    missingCredentials
            );
            remember(response);
            return response;
        }

        ProviderConnectivityResponse response = switch (provider.category()) {
            case "text-runtime" -> fromInference(provider, metadata, inferenceGatewayService.execute(new UnifiedInferenceRequest(
                    provider.code(),
                    provider.defaultModelCode(),
                    "Responda com a palavra OK.",
                    "ping",
                    null,
                    0.1,
                    32,
                    List.of(),
                    "connectivity-" + provider.code()
            )));
            case "research-search" -> {
                var researchResponse = researchService.query(new ResearchQueryRequest(provider.code(), "lume ai", 1));
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        researchResponse.status(),
                        researchResponse.configured(),
                        researchResponse.executionSupported(),
                        metadata.implementationStatus(),
                        metadata.evidenceLevel(),
                        providerCatalogService.streamingMode(provider),
                        providerCatalogService.runtimeMaturity(provider),
                        null,
                        researchResponse.error() == null ? "Connectivity test concluido." : researchResponse.error(),
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            case "vector-runtime" -> {
                var embeddingResponse = vectorCapabilityService.embeddings(new com.lume.workspace.dto.AiPlatformModels.EmbeddingRequest(
                        provider.code(),
                        null,
                        "connectivity test"
                ));
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        embeddingResponse.status(),
                        providerCatalogService.isConfigured(provider),
                        provider.executionSupported(),
                        metadata.implementationStatus(),
                        metadata.evidenceLevel(),
                        providerCatalogService.streamingMode(provider),
                        providerCatalogService.runtimeMaturity(provider),
                        null,
                        embeddingResponse.error() == null ? "Connectivity test concluido." : embeddingResponse.error(),
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            case "media-audio" -> {
                if ("deepgram".equals(provider.code())) {
                    var sttResponse = audioDocumentCapabilityService.speechToText(new com.lume.workspace.dto.AiPlatformModels.SpeechToTextRequest(
                            provider.code(),
                            null,
                            "https://dpgr.am/spacewalk.wav",
                            "en"
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            sttResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            sttResponse.error() == null ? "Connectivity test concluido." : sttResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("assemblyai".equals(provider.code())) {
                    var sttResponse = audioDocumentCapabilityService.speechToText(new com.lume.workspace.dto.AiPlatformModels.SpeechToTextRequest(
                            provider.code(),
                            null,
                            "https://assembly.ai/wildfires.mp3",
                            "en"
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            sttResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            sttResponse.error() == null ? "Connectivity test concluido." : sttResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("elevenlabs".equals(provider.code())) {
                    var ttsResponse = audioDocumentCapabilityService.textToSpeech(new com.lume.workspace.dto.AiPlatformModels.TextToSpeechRequest(
                            provider.code(),
                            null,
                            "ping",
                            null,
                            "mp3_44100_128"
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            ttsResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            ttsResponse.error() == null ? "Connectivity test concluido." : ttsResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("ideogram".equals(provider.code())) {
                    var imageResponse = mediaCapabilityService.generateImage(new com.lume.workspace.dto.AiPlatformModels.ImageGenerationRequest(
                            provider.code(),
                            null,
                            "Connectivity test image for Lume.",
                            "1:1",
                            null,
                            1,
                            null
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            imageResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            imageResponse.error() == null ? "Connectivity test concluido." : imageResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("stability-ai".equals(provider.code())) {
                    var imageResponse = mediaCapabilityService.generateImage(new com.lume.workspace.dto.AiPlatformModels.ImageGenerationRequest(
                            provider.code(),
                            null,
                            "Connectivity test image for Lume.",
                            "1:1",
                            null,
                            1,
                            null
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            imageResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            imageResponse.error() == null ? "Connectivity test concluido." : imageResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("bfl".equals(provider.code())) {
                    var imageResponse = mediaCapabilityService.generateImage(new com.lume.workspace.dto.AiPlatformModels.ImageGenerationRequest(
                            provider.code(),
                            "bfl:flux-2-klein-4b",
                            "Connectivity test image for Lume.",
                            "1:1",
                            null,
                            1,
                            null
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            imageResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            imageResponse.error() == null ? "Connectivity test concluiu o submit do job de imagem." : imageResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("replicate".equals(provider.code())) {
                    var imageResponse = mediaCapabilityService.generateImage(new com.lume.workspace.dto.AiPlatformModels.ImageGenerationRequest(
                            provider.code(),
                            null,
                            "Connectivity test image for Lume.",
                            "1:1",
                            null,
                            1,
                            null
                    ));
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            imageResponse.status(),
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            imageResponse.error() == null ? "Connectivity test concluiu o submit do job de imagem." : imageResponse.error(),
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                if ("runway".equals(provider.code())) {
                    yield new ProviderConnectivityResponse(
                            provider.code(),
                            provider.name(),
                            provider.category(),
                            provider.apiStyle(),
                            "manual_validation_recommended",
                            providerCatalogService.isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            providerCatalogService.streamingMode(provider),
                            providerCatalogService.runtimeMaturity(provider),
                            null,
                            "Provider de video assincrono integrado; valide com um submit controlado em /api/v1/videos/generate.",
                            providerCatalogService.missingCredentialEnvVars(provider)
                    );
                }
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        "unsupported",
                        providerCatalogService.isConfigured(provider),
                        provider.executionSupported(),
                        metadata.implementationStatus(),
                        metadata.evidenceLevel(),
                        providerCatalogService.streamingMode(provider),
                        providerCatalogService.runtimeMaturity(provider),
                        null,
                        "Connectivity test ainda nao existe para este provider de media/audio.",
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            case "threat-intel" -> {
                var threatResponse = threatIntelService.query(new ThreatIntelQueryRequest(provider.code(), "lume", 1, "Provider connectivity test"));
                yield new ProviderConnectivityResponse(
                        provider.code(),
                        provider.name(),
                        provider.category(),
                        provider.apiStyle(),
                        threatResponse.status(),
                        threatResponse.configured(),
                        threatResponse.executionSupported(),
                        metadata.implementationStatus(),
                        metadata.evidenceLevel(),
                        providerCatalogService.streamingMode(provider),
                        providerCatalogService.runtimeMaturity(provider),
                        null,
                        threatResponse.error(),
                        providerCatalogService.missingCredentialEnvVars(provider)
                );
            }
            default -> new ProviderConnectivityResponse(
                    provider.code(),
                    provider.name(),
                    provider.category(),
                    provider.apiStyle(),
                    "unsupported",
                    providerCatalogService.isConfigured(provider),
                    provider.executionSupported(),
                    metadata.implementationStatus(),
                    metadata.evidenceLevel(),
                    providerCatalogService.streamingMode(provider),
                    providerCatalogService.runtimeMaturity(provider),
                    null,
                    "Connectivity test ainda nao existe para esta categoria.",
                    providerCatalogService.missingCredentialEnvVars(provider)
            );
        };
        remember(response);
        return response;
    }

    private ProviderConnectivityResponse fromInference(
            ProviderDefinition provider,
            ProviderGovernanceMetadata metadata,
            UnifiedInferenceResponse response
    ) {
        return new ProviderConnectivityResponse(
                provider.code(),
                provider.name(),
                provider.category(),
                provider.apiStyle(),
                response.status(),
                response.configured(),
                response.executionSupported(),
                metadata.implementationStatus(),
                metadata.evidenceLevel(),
                response.streamingMode(),
                providerCatalogService.runtimeMaturity(provider),
                response.latencyMs(),
                response.error() == null ? "Connectivity test concluido." : response.error(),
                providerCatalogService.missingCredentialEnvVars(provider)
        );
    }

    public Map<String, ProviderConnectivitySnapshot> lastConnectivitySnapshots() {
        return Map.copyOf(snapshots);
    }

    private void remember(ProviderConnectivityResponse response) {
        snapshots.put(
                response.providerCode(),
                new ProviderConnectivitySnapshot(
                        response.providerCode(),
                        response.status(),
                        response.message(),
                        OffsetDateTime.now()
                )
        );
    }
}
