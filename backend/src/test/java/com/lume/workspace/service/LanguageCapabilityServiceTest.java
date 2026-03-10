package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
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

@DisplayName("LanguageCapabilityService")
class LanguageCapabilityServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should translate text with Google Translation")
    void shouldTranslateText() {
        ProviderCatalogService catalogService = catalogService(new MockEnvironment()
                .withProperty("GOOGLE_CLOUD_CREDENTIALS_JSON", "{\"project_id\":\"lume-test\"}"));
        WorkspaceContextService workspaceContextService = new TestWorkspaceContextService();
        AuditLogService auditLogService = new NoOpAuditLogService();
        GoogleCloudSupportService googleCloudSupportService = new StubGoogleCloudSupportService(catalogService, "google-token", "lume-test");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://translation.googleapis.com/v3/projects/lume-test/locations/global:translateText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer google-token"))
                .andRespond(withSuccess("""
                        {"translations":[{"translatedText":"hello","detectedLanguageCode":"pt-BR"}]}
                        """, MediaType.APPLICATION_JSON));

        LanguageCapabilityService service = new LanguageCapabilityService(
                catalogService,
                workspaceContextService,
                auditLogService,
                googleCloudSupportService,
                builder,
                objectMapper
        );

        AiPlatformModels.TranslationResponse response = service.translate(new AiPlatformModels.TranslationRequest(
                "google-translation",
                null,
                "ola",
                "pt-BR",
                "en"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.translatedText()).isEqualTo("hello");
        assertThat(response.detectedLanguageCode()).isEqualTo("pt-BR");
    }

    @Test
    @DisplayName("should analyze text with Google Natural Language")
    void shouldAnalyzeText() {
        ProviderCatalogService catalogService = catalogService(new MockEnvironment()
                .withProperty("GOOGLE_CLOUD_CREDENTIALS_JSON", "{\"project_id\":\"lume-test\"}"));
        WorkspaceContextService workspaceContextService = new TestWorkspaceContextService();
        AuditLogService auditLogService = new NoOpAuditLogService();
        GoogleCloudSupportService googleCloudSupportService = new StubGoogleCloudSupportService(catalogService, "google-token", "lume-test");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://language.googleapis.com/v2/documents:analyzeEntities"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer google-token"))
                .andRespond(withSuccess("""
                        {"language":"pt","entities":[{"name":"Lume","type":"ORGANIZATION","salience":0.7}]}
                        """, MediaType.APPLICATION_JSON));

        LanguageCapabilityService service = new LanguageCapabilityService(
                catalogService,
                workspaceContextService,
                auditLogService,
                googleCloudSupportService,
                builder,
                objectMapper
        );

        AiPlatformModels.NlpAnalysisResponse response = service.analyze(new AiPlatformModels.NlpAnalysisRequest(
                "google-natural-language",
                null,
                "Lume ajuda equipes.",
                "entities",
                "pt"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.languageCode()).isEqualTo("pt");
        assertThat(response.entities()).hasSize(1);
        assertThat(response.entities().get(0).name()).isEqualTo("Lume");
    }

    private ProviderCatalogService catalogService(MockEnvironment environment) {
        return new ProviderCatalogService(new EnvironmentSecretResolver(environment));
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

    private static final class StubGoogleCloudSupportService extends GoogleCloudSupportService {

        private final GoogleAccessContext accessContext;

        private StubGoogleCloudSupportService(ProviderCatalogService catalogService, String accessToken, String projectId) {
            super(catalogService, new ObjectMapper());
            this.accessContext = new GoogleAccessContext(accessToken, projectId);
        }

        @Override
        public GoogleAccessContext accessContext(com.lume.workspace.inference.ProviderDefinition provider) {
            return accessContext;
        }
    }
}
