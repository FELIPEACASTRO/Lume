package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.dto.ResearchQueryRequest;
import com.lume.workspace.dto.ResearchQueryResponse;
import com.lume.workspace.dto.ResearchResultItemResponse;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.orchestration.AiExecutionAttempt;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiCapabilityService - Unit Tests")
class AiCapabilityServiceTest {

    @Test
    @DisplayName("should map attempt chain and provider aliases for chat responses")
    void shouldMapAttemptChainAndProviderAliasesForChatResponses() {
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(new MockEnvironment()));
        StubInferenceGatewayService inferenceGatewayService = new StubInferenceGatewayService(new UnifiedInferenceResponse(
                "openai",
                "OpenAI",
                "openai:gpt-4.1-mini",
                "agent-v1-openai",
                "responses",
                true,
                true,
                false,
                "completed",
                "Resposta pronta",
                null,
                "openai",
                List.of("openai"),
                List.of(new AiExecutionAttempt("openai", "completed", null, 42L)),
                42L,
                12,
                18,
                0.0012,
                "native",
                "quality-first"
        ));

        AiCapabilityService service = new AiCapabilityService(
                inferenceGatewayService,
                catalogService,
                new NoOpVectorCapabilityService(catalogService),
                new NoOpAudioDocumentCapabilityService(catalogService),
                new NoOpMediaCapabilityService(catalogService),
                new NoOpResearchService(catalogService),
                new NoOpThreatIntelService(catalogService),
                new NoOpLanguageCapabilityService(catalogService)
        );

        AiPlatformModels.ChatResponse response = service.chat(new AiPlatformModels.ChatRequest(
                "openai",
                null,
                "Seja direto.",
                "Resuma o workspace.",
                null,
                0.2,
                200,
                List.of(),
                false,
                "req-chat",
                "quality-first",
                true,
                List.of("roadmap", "workspace"),
                "ws-main"
        ));

        assertThat(inferenceGatewayService.lastRequest()).isNotNull();
        assertThat(inferenceGatewayService.lastRequest().routingMode()).isEqualTo("quality-first");
        assertThat(inferenceGatewayService.lastRequest().workspaceId()).isEqualTo("ws-main");
        assertThat(response.providerCode()).isEqualTo("openai");
        assertThat(response.requestedProviderCode()).isEqualTo("openai");
        assertThat(response.providerUsed()).isEqualTo("openai");
        assertThat(response.modelUsed()).isEqualTo("openai:gpt-4.1-mini");
        assertThat(response.streamingMode()).isEqualTo("native");
        assertThat(response.streamingSupported()).isTrue();
        assertThat(response.attemptChain()).hasSize(1);
        assertThat(response.attemptChain().getFirst().status()).isEqualTo("completed");
    }

    @Test
    @DisplayName("should enrich grounded chat with research citations when a research provider is supplied")
    void shouldEnrichGroundedChatWithResearchCitationsWhenResearchProviderIsSupplied() {
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(new MockEnvironment()));
        StubInferenceGatewayService inferenceGatewayService = new StubInferenceGatewayService(new UnifiedInferenceResponse(
                "openai",
                "OpenAI",
                "openai:gpt-4.1-mini",
                "agent-v1-openai",
                "responses",
                true,
                true,
                false,
                "completed",
                "Resposta grounded",
                null,
                "openai",
                List.of("openai"),
                List.of(new AiExecutionAttempt("openai", "completed", null, 55L)),
                55L,
                14,
                22,
                0.0018,
                "unsupported",
                "quality-first"
        ));
        StubResearchService researchService = new StubResearchService(
                catalogService,
                new ResearchQueryResponse(
                        "exa",
                        "Exa",
                        true,
                        true,
                        "completed",
                        "mercado de IA",
                        List.of(new ResearchResultItemResponse(
                                "Mercado de IA em 2026",
                                "https://example.com/ai-2026",
                                "A adocao de copilots corporativos acelerou em 2026.",
                                "Exa",
                                0.93
                        )),
                        null
                )
        );

        AiCapabilityService service = new AiCapabilityService(
                inferenceGatewayService,
                catalogService,
                new NoOpVectorCapabilityService(catalogService),
                new NoOpAudioDocumentCapabilityService(catalogService),
                new NoOpMediaCapabilityService(catalogService),
                researchService,
                new NoOpThreatIntelService(catalogService),
                new NoOpLanguageCapabilityService(catalogService)
        );

        AiPlatformModels.WebGroundedChatResponse response = service.webGroundedChat(new AiPlatformModels.WebGroundedChatRequest(
                "openai",
                null,
                "Responda com base em evidencias.",
                "Quais sao os principais sinais do mercado de IA?",
                null,
                0.2,
                200,
                List.of(),
                "req-grounded",
                "quality-first",
                false,
                List.of("mercado", "grounded"),
                "ws-main",
                "exa",
                3
        ));

        assertThat(researchService.lastRequest()).isNotNull();
        assertThat(researchService.lastRequest().providerCode()).isEqualTo("exa");
        assertThat(inferenceGatewayService.lastRequest()).isNotNull();
        assertThat(inferenceGatewayService.lastRequest().prompt()).contains("Contexto web verificado:");
        assertThat(inferenceGatewayService.lastRequest().prompt()).contains("Mercado de IA em 2026");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.citations().getFirst().url()).isEqualTo("https://example.com/ai-2026");
        assertThat(response.grounding()).hasSize(1);
        assertThat(response.grounding().getFirst().providerCode()).isEqualTo("exa");
        assertThat(response.content()).isEqualTo("Resposta grounded");
    }

    @Test
    @DisplayName("should short-circuit grounded chat when research provider fails before inference")
    void shouldShortCircuitGroundedChatWhenResearchProviderFailsBeforeInference() {
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(new MockEnvironment()));
        StubInferenceGatewayService inferenceGatewayService = new StubInferenceGatewayService(new UnifiedInferenceResponse(
                "openai",
                "OpenAI",
                "openai:gpt-4.1-mini",
                "agent-v1-openai",
                "responses",
                true,
                true,
                false,
                "completed",
                "Nao deve ser usado",
                null,
                "openai",
                List.of("openai"),
                List.of(),
                10L,
                1,
                1,
                0.0,
                "unsupported",
                "quality-first"
        ));
        StubResearchService researchService = new StubResearchService(
                catalogService,
                new ResearchQueryResponse(
                        "exa",
                        "Exa",
                        false,
                        true,
                        "missing_credentials",
                        "mercado de IA",
                        List.of(),
                        "Credenciais ausentes: EXA_API_KEY"
                )
        );

        AiCapabilityService service = new AiCapabilityService(
                inferenceGatewayService,
                catalogService,
                new NoOpVectorCapabilityService(catalogService),
                new NoOpAudioDocumentCapabilityService(catalogService),
                new NoOpMediaCapabilityService(catalogService),
                researchService,
                new NoOpThreatIntelService(catalogService),
                new NoOpLanguageCapabilityService(catalogService)
        );

        AiPlatformModels.WebGroundedChatResponse response = service.webGroundedChat(new AiPlatformModels.WebGroundedChatRequest(
                "openai",
                null,
                null,
                "Quais sao os principais sinais do mercado de IA?",
                null,
                0.2,
                200,
                List.of(),
                "req-grounded-missing",
                "quality-first",
                false,
                List.of(),
                "ws-main",
                "exa",
                3
        ));

        assertThat(inferenceGatewayService.lastRequest()).isNull();
        assertThat(response.status()).isEqualTo("missing_credentials");
        assertThat(response.error()).isEqualTo("Credenciais ausentes: EXA_API_KEY");
    }

    private static final class StubInferenceGatewayService extends InferenceGatewayService {
        private final UnifiedInferenceResponse response;
        private UnifiedInferenceRequest lastRequest;

        private StubInferenceGatewayService(UnifiedInferenceResponse response) {
            super(null);
            this.response = response;
        }

        @Override
        public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
            this.lastRequest = request;
            return response;
        }

        private UnifiedInferenceRequest lastRequest() {
            return lastRequest;
        }
    }

    private static final class StubResearchService extends ResearchService {
        private final ResearchQueryResponse response;
        private ResearchQueryRequest lastRequest;

        private StubResearchService(ProviderCatalogService providerCatalogService, ResearchQueryResponse response) {
            super(
                    providerCatalogService,
                    new TestWorkspaceContextService(),
                    new NoOpAuditLogService(),
                    new NoOpWorkspaceLedgerService(),
                    RestClient.builder(),
                    new ObjectMapper()
            );
            this.response = response;
        }

        @Override
        public ResearchQueryResponse query(ResearchQueryRequest request) {
            this.lastRequest = request;
            return response;
        }

        private ResearchQueryRequest lastRequest() {
            return lastRequest;
        }
    }

    private static final class NoOpVectorCapabilityService extends VectorCapabilityService {
        private NoOpVectorCapabilityService(ProviderCatalogService providerCatalogService) {
            super(providerCatalogService, new TestWorkspaceContextService(), new NoOpAuditLogService(), RestClient.builder(), new ObjectMapper());
        }
    }

    private static final class NoOpThreatIntelService extends ThreatIntelService {
        private NoOpThreatIntelService(ProviderCatalogService providerCatalogService) {
            super(providerCatalogService, new TestWorkspaceContextService(), new NoOpAuditLogService(), new SecurityComplianceProperties(), RestClient.builder(), new ObjectMapper());
        }

        @Override
        public ThreatIntelQueryResponse query(ThreatIntelQueryRequest request) {
            throw new UnsupportedOperationException("Nao usado neste teste.");
        }
    }

    private static final class NoOpAudioDocumentCapabilityService extends AudioDocumentCapabilityService {
        private NoOpAudioDocumentCapabilityService(ProviderCatalogService providerCatalogService) {
            super(providerCatalogService, new TestWorkspaceContextService(), new NoOpAuditLogService(), new GoogleCloudSupportService(providerCatalogService, new ObjectMapper()), RestClient.builder(), new ObjectMapper());
        }
    }

    private static final class NoOpMediaCapabilityService extends MediaCapabilityService {
        private NoOpMediaCapabilityService(ProviderCatalogService providerCatalogService) {
            super(providerCatalogService, new TestWorkspaceContextService(), new NoOpAuditLogService(), RestClient.builder(), new ObjectMapper());
        }
    }

    private static final class NoOpAuditLogService extends AuditLogService {
        private NoOpAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }
    }

    private static final class TestWorkspaceContextService extends WorkspaceContextService {
        private TestWorkspaceContextService() {
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

    private static final class NoOpResearchService extends ResearchService {
        private NoOpResearchService(ProviderCatalogService providerCatalogService) {
            super(
                    providerCatalogService,
                    new TestWorkspaceContextService(),
                    new NoOpAuditLogService(),
                    new NoOpWorkspaceLedgerService(),
                    RestClient.builder(),
                    new ObjectMapper()
            );
        }
    }

    private static final class NoOpLanguageCapabilityService extends LanguageCapabilityService {
        private NoOpLanguageCapabilityService(ProviderCatalogService providerCatalogService) {
            super(providerCatalogService, new TestWorkspaceContextService(), new NoOpAuditLogService(), new GoogleCloudSupportService(providerCatalogService, new ObjectMapper()), RestClient.builder(), new ObjectMapper());
        }

        @Override
        public AiPlatformModels.TranslationResponse translate(AiPlatformModels.TranslationRequest request) {
            throw new UnsupportedOperationException("Nao usado neste teste.");
        }

        @Override
        public AiPlatformModels.NlpAnalysisResponse analyze(AiPlatformModels.NlpAnalysisRequest request) {
            throw new UnsupportedOperationException("Nao usado neste teste.");
        }
    }
}
