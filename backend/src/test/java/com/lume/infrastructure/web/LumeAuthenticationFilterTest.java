package com.lume.infrastructure.web;

import com.lume.infrastructure.config.WorkspaceAuthProperties;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.workspace.service.WorkspaceSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("LumeAuthenticationFilter - Unit Tests")
class LumeAuthenticationFilterTest {

    private StubSessionService sessionService;
    private WorkspaceAuthProperties authProperties;
    private FilterChain filterChain;
    private LumeAuthenticationFilter filter;
    private Environment mockEnvironment;

    @BeforeEach
    void setUp() {
        mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"test"});
        sessionService = new StubSessionService(mockEnvironment);
        authProperties = new WorkspaceAuthProperties(mockEnvironment);
        filterChain = mock(FilterChain.class);
        filter = new LumeAuthenticationFilter(sessionService, authProperties);
    }

    @Test
    @DisplayName("Public exact paths bypass authentication")
    void shouldAllowPublicExactPaths() throws Exception {
        String[] exactPaths = {
                "/v1/auth/login",
                "/v1/auth/logout",
                "/v1/auth/session",
                "/v1/setup/status",
                "/v1/setup/bootstrap"
        };

        for (String path : exactPaths) {
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        assertThat(sessionService.resolveCallCount).isZero();
    }

    @Test
    @DisplayName("Public prefix paths bypass authentication")
    void shouldAllowPublicPrefixPaths() throws Exception {
        String[] prefixPaths = {
                "/v1/auth/whatever",
                "/health",
                "/actuator/health",
                "/v3/api-docs",
                "/swagger-ui/index.html"
        };

        for (String path : prefixPaths) {
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
        }

        assertThat(sessionService.resolveCallCount).isZero();
    }

    @Test
    @DisplayName("OPTIONS requests bypass authentication for CORS preflight")
    void shouldAllowOptionsBypassForCors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/v1/agents");
        request.setServletPath("/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(sessionService.resolveCallCount).isZero();
    }

    @Test
    @DisplayName("Test actor header is accepted when allow-test-header is enabled")
    void shouldAllowTestHeaderWhenEnabled() throws Exception {
        authProperties.setAllowTestHeader(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        request.addHeader("X-Lume-Actor-User-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(sessionService.resolveCallCount).isZero();
    }

    @Test
    @DisplayName("Test actor header is rejected when allow-test-header is disabled")
    void shouldRejectTestHeaderWhenDisabled() throws Exception {
        authProperties.setAllowTestHeader(false);
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        request.addHeader("X-Lume-Actor-User-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Blank test actor header is treated as absent")
    void shouldRejectBlankTestHeader() throws Exception {
        authProperties.setAllowTestHeader(true);
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        request.addHeader("X-Lume-Actor-User-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Test auto-login bypasses session enforcement when enabled")
    void shouldAllowTestAutoLoginWhenEnabled() throws Exception {
        authProperties.setAllowTestAutoLogin(true);
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(sessionService.resolveCallCount).isZero();
    }

    @Test
    @DisplayName("Unauthenticated request receives 401 JSON response")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":401");
        assertThat(response.getContentAsString()).contains("\"message\":");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authenticated session allows request through")
    void shouldAllowAuthenticatedSession() throws Exception {
        sessionService.authenticatedUser = Optional.of(new UserJpaEntity());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Near-miss path /v1/authz is not treated as public")
    void shouldRejectNearMissPath() throws Exception {
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/authz");
        request.setServletPath("/v1/authz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Test header bypass is blocked when production profile is active")
    void shouldBlockTestHeaderInProductionProfile() throws Exception {
        when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        authProperties.setAllowTestHeader(true);
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        request.addHeader("X-Lume-Actor-User-Id", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Auto-login bypass is blocked when production profile is active")
    void shouldBlockAutoLoginInProductionProfile() throws Exception {
        when(mockEnvironment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        authProperties.setAllowTestAutoLogin(true);
        sessionService.authenticatedUser = Optional.empty();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/agents");
        request.setServletPath("/v1/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ---- Test double ----

    private static class StubSessionService extends WorkspaceSessionService {
        Optional<UserJpaEntity> authenticatedUser = Optional.empty();
        int resolveCallCount = 0;

        StubSessionService(Environment environment) {
            super(null, null, new WorkspaceAuthProperties(environment));
        }

        @Override
        public Optional<UserJpaEntity> resolveAuthenticatedUser(HttpServletRequest request) {
            resolveCallCount++;
            return authenticatedUser;
        }
    }
}
