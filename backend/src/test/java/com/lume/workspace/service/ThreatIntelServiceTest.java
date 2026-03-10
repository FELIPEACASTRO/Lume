package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.config.SecurityComplianceProperties;
import com.lume.workspace.dto.ThreatIntelQueryRequest;
import com.lume.workspace.dto.ThreatIntelQueryResponse;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ThreatIntelService")
class ThreatIntelServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should query FullHunt compromised credentials")
    void shouldQueryFullHunt() {
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("FULLHUNT_API_KEY", "fh-key"));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        service = service(new MockEnvironment().withProperty("FULLHUNT_API_KEY", "fh-key"), builder);

        server.expect(requestTo("https://fullhunt.io/api/v1/enterprise/darkweb/compromised-credentials?query=lume&page=1&size=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-api-key", "fh-key"))
                .andRespond(withSuccess("""
                        {"results":[{"email":"ops@lume.local","database_name":"db-breach","password":"***"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("fullhunt", "lume", 2, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).contains("ops@lume.local");
    }

    @Test
    @DisplayName("should query Flare via temporary token")
    void shouldQueryFlare() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("FLARE_API_KEY", "flare-key")
                .withProperty("FLARE_TENANT_ID", "tenant-1"), builder);

        server.expect(requestTo("https://api.flare.io/tokens/generate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "flare-key"))
                .andRespond(withSuccess("""
                        {"token":"flare-token"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://api.flare.io/firework/v4/events/global/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer flare-token"))
                .andRespond(withSuccess("""
                        {"items":[{"metadata":{"uid":"evt-1","source_type":"forum_post"},"rendered_message":"resultado flare"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("flare", "lume", 1, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).excerpt()).contains("resultado flare");
    }

    @Test
    @DisplayName("should query DarkOwl with signed headers")
    void shouldQueryDarkOwl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("DARKOWL_PUBLIC_KEY", "public-key")
                .withProperty("DARKOWL_PRIVATE_KEY", "private-key"), builder);

        server.expect(requestTo("https://api.darkowl.com/api/v1/search?q=lume&detail=snippet&count=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(request -> {
                    String auth = request.getHeaders().getFirst("X-DarkOwl-Authorization");
                    assertThat(auth).startsWith("public-key:");
                    assertThat(request.getHeaders().getFirst("X-DarkOwl-Date")).isNotBlank();
                })
                .andRespond(withSuccess("""
                        {"results":[{"title":"post","snippet":"darkowl resultado","url":"https://dark.example/item"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("darkowl", "lume", 1, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).reference()).contains("dark.example");
    }

    @Test
    @DisplayName("should query Onion Search Engine via official api.php endpoint")
    void shouldQueryOnionSearchEngine() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("ONION_SEARCH_API_KEY", "onion-key"), builder);

        server.expect(requestTo("https://onionsearchengine.com/api.php?q=lume&limit=2&type=search"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-api-key", "onion-key"))
                .andRespond(withSuccess("""
                        {"results":[{"title":"Forum thread","description":"onion resultado","url":"http://abc.onion/thread"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("onion-search-engine", "lume", 2, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).reference()).contains(".onion");
    }

    @Test
    @DisplayName("should query Twingly Dark Web Search API")
    void shouldQueryTwingly() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("TWINGLY_API_KEY", "twingly-key"), builder);

        server.expect(requestTo("https://data.twingly.net/darkweb/a/search/v1/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "apikey twingly-key"))
                .andExpect(header("Content-Type", "application/json; charset=utf-8"))
                .andExpect(header("Accept", "application/json; charset=utf-8"))
                .andRespond(withSuccess("""
                        {"documents":[{"uuid":"doc-1","site_name":"Mercado X","text":"resultado twingly","url":"http://market.onion/post","title":"Oferta"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("twingly", "lume", 1, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).excerpt()).contains("resultado twingly");
    }

    @Test
    @DisplayName("should query DarknetSearch/Kaduu via OAuth password flow")
    void shouldQueryDarknetSearch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ThreatIntelService service = service(new MockEnvironment()
                .withProperty("DARKNETSEARCH_USERNAME", "demo-user")
                .withProperty("DARKNETSEARCH_PASSWORD", "demo-pass")
                .withProperty("DARKNETSEARCH_CLIENT_ID", "client-id")
                .withProperty("DARKNETSEARCH_CLIENT_SECRET", "client-secret"), builder);

        server.expect(requestTo("https://app.leak.center/uaa/oauth/token?grant_type=password&username=demo-user&password=demo-pass&scope=openid"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ="))
                .andRespond(withSuccess("""
                        {"access_token":"kaduu-token"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://client-api.leak.center/api/service/leak_extended_database_search/?page=0&size=2&query=lume&highlight=true&length=300"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer kaduu-token"))
                .andRespond(withSuccess("""
                        {"content":[{"fileName":"dump.sql","website":"market.onion","highlightedContent":"darknetsearch resultado"}]}
                        """, MediaType.APPLICATION_JSON));

        ThreatIntelQueryResponse response = service.query(new ThreatIntelQueryRequest("darknetsearch", "lume", 2, "Investigacao autorizada"));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).excerpt()).contains("darknetsearch resultado");
    }

    private ThreatIntelService service(MockEnvironment environment) {
        return service(environment, RestClient.builder());
    }

    private ThreatIntelService service(MockEnvironment environment, RestClient.Builder builder) {
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        WorkspaceContextService workspaceContextService = new TestWorkspaceContextService();
        AuditLogService auditLogService = new NoOpAuditLogService();
        SecurityComplianceProperties securityComplianceProperties = new SecurityComplianceProperties();
        securityComplianceProperties.setDarkWebEnabled(true);
        return new ThreatIntelService(
                catalogService,
                workspaceContextService,
                auditLogService,
                securityComplianceProperties,
                builder,
                objectMapper
        );
    }

    private static final class TestWorkspaceContextService extends WorkspaceContextService {
        private TestWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public HttpServletRequest getObject() {
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
        private NoOpAuditLogService() {
            super(null, new TestWorkspaceContextService(), new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }

        @Override
        public void recordExplicit(Long organizationId, Long workspaceId, Long actorUserId, String entityType, String entityId, String action, Object payload) {
        }
    }
}
