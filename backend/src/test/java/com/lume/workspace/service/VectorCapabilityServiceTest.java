package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("VectorCapabilityService - Unit Tests")
class VectorCapabilityServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should return cohere embeddings")
    void shouldReturnCohereEmbeddings() {
        MockEnvironment environment = new MockEnvironment().withProperty("COHERE_API_KEY", "test-cohere");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.cohere.com/v2/embed"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-cohere"))
                .andRespond(withSuccess("""
                        {
                          "embeddings": {
                            "float": [[0.12, 0.34, 0.56]]
                          },
                          "meta": {
                            "billed_units": {
                              "input_tokens": 8
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        VectorCapabilityService service = service(catalogService, builder);
        AiPlatformModels.EmbeddingResponse response = service.embeddings(new AiPlatformModels.EmbeddingRequest(
                "cohere",
                null,
                "documento de teste"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("cohere:embed-v4.0");
        assertThat(response.dimensions()).isEqualTo(3);
        assertThat(response.embeddings()).hasSize(1);
        assertThat(response.usage().inputTokens()).isEqualTo(8);
    }

    @Test
    @DisplayName("should return voyage rerank")
    void shouldReturnVoyageRerank() {
        MockEnvironment environment = new MockEnvironment().withProperty("VOYAGE_API_KEY", "test-voyage");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.voyageai.com/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-voyage"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"index": 1, "relevance_score": 0.92},
                            {"index": 0, "relevance_score": 0.87}
                          ],
                          "usage": {
                            "total_tokens": 14
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        VectorCapabilityService service = service(catalogService, builder);
        AiPlatformModels.RerankResponse response = service.rerank(new AiPlatformModels.RerankRequest(
                "voyage-ai",
                null,
                "qual documento fala de custo?",
                java.util.List.of("plano de produto", "orcamento e custos"),
                2
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("voyage-ai:rerank-2");
        assertThat(response.rankedIndexes()).containsExactly(1, 0);
        assertThat(response.rankedDocuments()).containsExactly("orcamento e custos", "plano de produto");
        assertThat(response.usage().inputTokens()).isEqualTo(14);
    }

    @Test
    @DisplayName("should return dashscope embeddings")
    void shouldReturnDashScopeEmbeddings() {
        MockEnvironment environment = new MockEnvironment().withProperty("DASHSCOPE_API_KEY", "test-dashscope");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-dashscope"))
                .andRespond(withSuccess("""
                        {
                          "data": [
                            {"embedding": [0.11, 0.22, 0.33]}
                          ],
                          "usage": {
                            "prompt_tokens": 6
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        VectorCapabilityService service = service(catalogService, builder);
        AiPlatformModels.EmbeddingResponse response = service.embeddings(new AiPlatformModels.EmbeddingRequest(
                "dashscope-qwen",
                null,
                "documento com contexto"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("dashscope-qwen:text-embedding-v4");
        assertThat(response.dimensions()).isEqualTo(3);
        assertThat(response.usage().inputTokens()).isEqualTo(6);
    }

    @Test
    @DisplayName("should return siliconflow rerank")
    void shouldReturnSiliconFlowRerank() {
        MockEnvironment environment = new MockEnvironment().withProperty("SILICONFLOW_API_KEY", "test-sf");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.siliconflow.cn/v1/rerank"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-sf"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {"index": 1, "relevance_score": 0.91},
                            {"index": 0, "relevance_score": 0.73}
                          ],
                          "usage": {
                            "total_tokens": 12
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        VectorCapabilityService service = service(catalogService, builder);
        AiPlatformModels.RerankResponse response = service.rerank(new AiPlatformModels.RerankRequest(
                "siliconflow",
                null,
                "qual documento fala de custo?",
                java.util.List.of("plano de produto", "orcamento e custos"),
                2
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("siliconflow:BAAI/bge-reranker-v2-m3");
        assertThat(response.rankedIndexes()).containsExactly(1, 0);
        assertThat(response.rankedDocuments()).containsExactly("orcamento e custos", "plano de produto");
        assertThat(response.usage().inputTokens()).isEqualTo(12);
    }

    private VectorCapabilityService service(ProviderCatalogService catalogService, RestClient.Builder builder) {
        return new VectorCapabilityService(
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
