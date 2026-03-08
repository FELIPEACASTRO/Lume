package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.ResearchQueryRequest;
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

@DisplayName("ResearchService - Unit Tests")
class ResearchServiceTest {

    @Test
    @DisplayName("should query tavily search")
    void shouldQueryTavilySearch() {
        MockEnvironment environment = new MockEnvironment().withProperty("TAVILY_API_KEY", "test-tavily");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.tavily.com/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-tavily"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "title": "Lume AI",
                              "url": "https://example.com/lume",
                              "content": "Resumo do resultado",
                              "score": 0.91
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ResearchService service = service(catalogService, builder);
        var response = service.query(new ResearchQueryRequest("tavily", "lume ai", 5));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().title()).isEqualTo("Lume AI");
    }

    @Test
    @DisplayName("should query serpapi search")
    void shouldQuerySerpApiSearch() {
        MockEnvironment environment = new MockEnvironment().withProperty("SERPAPI_API_KEY", "test-serpapi");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://serpapi.com/search.json?engine=google&q=lume%20ai&num=3&api_key=test-serpapi"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "organic_results": [
                            {
                              "position": 1,
                              "title": "Lume Platform",
                              "link": "https://example.com/platform",
                              "snippet": "Descricao resumida"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ResearchService service = service(catalogService, builder);
        var response = service.query(new ResearchQueryRequest("serpapi", "lume ai", 3));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().url()).isEqualTo("https://example.com/platform");
    }

    private ResearchService service(ProviderCatalogService catalogService, RestClient.Builder builder) {
        return new ResearchService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                builder,
                new ObjectMapper()
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
