package com.lume.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterFilterTest {

    private StubLoginRateLimitService rateLimitService;
    private LoginRateLimiterFilter filter;
    private RecordingFilterChain chain;

    @BeforeEach
    void setUp() {
        rateLimitService = new StubLoginRateLimitService();
        filter = new LoginRateLimiterFilter(rateLimitService, new ObjectMapper(), 10, 60);
        chain = new RecordingFilterChain();
    }

    private MockHttpServletRequest postLogin(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/auth/login");
        request.setServletPath("/v1/auth/login");
        request.setRemoteAddr(remoteAddr);
        request.setContentType("application/json");
        return request;
    }

    @Test
    @DisplayName("GET /v1/auth/login should not be filtered")
    void shouldNotFilterGetLoginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/auth/login");
        request.setServletPath("/v1/auth/login");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("POST to non-login endpoint should not be filtered")
    void shouldNotFilterPostToOtherEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/agents");
        request.setServletPath("/v1/agents");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("login request should pass through and emit rate headers when allowed")
    void shouldAllowAttemptAndSetHeaders() throws ServletException, IOException {
        rateLimitService.nextDecision = new LoginRateLimitService.LoginRateLimitDecision(false, 9, 60, 10);

        MockHttpServletRequest request = postLogin("192.168.1.100");
        request.setContent("{\"email\":\"operator@lume.local\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(rateLimitService.lastClientIp).isEqualTo("192.168.1.100");
        assertThat(rateLimitService.lastIdentity).isEqualTo("operator@lume.local");
        assertThat(rateLimitService.lastMaxAttempts).isEqualTo(10);
        assertThat(rateLimitService.lastWindowSeconds).isEqualTo(60);
        assertThat(chain.invocationCount).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    @DisplayName("blocked attempt should return 429 with retry metadata")
    void shouldBlockAttemptWith429() throws ServletException, IOException {
        rateLimitService.nextDecision = new LoginRateLimitService.LoginRateLimitDecision(true, 0, 27, 10);

        MockHttpServletRequest request = postLogin("192.168.1.100");
        request.setContent("{\"email\":\"operator@lume.local\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(rateLimitService.lastClientIp).isEqualTo("192.168.1.100");
        assertThat(rateLimitService.lastIdentity).isEqualTo("operator@lume.local");
        assertThat(chain.invocationCount).isZero();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("Retry-After")).isEqualTo("27");
        assertThat(response.getContentAsString()).contains("Muitas tentativas de login");
    }

    @Test
    @DisplayName("should use first forwarded IP and normalized email identity")
    void shouldUseForwardedIpAndNormalizedIdentity() throws ServletException, IOException {
        rateLimitService.nextDecision = new LoginRateLimitService.LoginRateLimitDecision(false, 8, 60, 10);

        MockHttpServletRequest request = postLogin("127.0.0.1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request.setContent("{\"email\":\" Operator@Lume.Local \"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(rateLimitService.lastClientIp).isEqualTo("10.0.0.1");
        assertThat(rateLimitService.lastIdentity).isEqualTo("operator@lume.local");
        assertThat(chain.invocationCount).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("should allow anonymous payload when email is missing")
    void shouldAllowAnonymousIdentityWhenPayloadDoesNotContainEmail() throws ServletException, IOException {
        rateLimitService.nextDecision = new LoginRateLimitService.LoginRateLimitDecision(false, 9, 60, 10);

        MockHttpServletRequest request = postLogin("192.168.1.200");
        request.setContent("{\"username\":\"operator\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(rateLimitService.lastClientIp).isEqualTo("192.168.1.200");
        assertThat(rateLimitService.lastIdentity).isNull();
        assertThat(chain.invocationCount).isEqualTo(1);
    }

    private static final class RecordingFilterChain implements FilterChain {
        private int invocationCount;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocationCount++;
        }
    }

    private static final class StubLoginRateLimitService extends LoginRateLimitService {
        private LoginRateLimitDecision nextDecision = new LoginRateLimitDecision(false, 10, 60, 10);
        private String lastClientIp;
        private String lastIdentity;
        private int lastMaxAttempts;
        private int lastWindowSeconds;

        private StubLoginRateLimitService() {
            super(null, Clock.fixed(Instant.parse("2026-03-12T12:00:00Z"), ZoneOffset.UTC));
        }

        @Override
        public LoginRateLimitDecision registerAttempt(String clientIp, String identity, int maxAttempts, int windowSeconds) {
            this.lastClientIp = clientIp;
            this.lastIdentity = identity;
            this.lastMaxAttempts = maxAttempts;
            this.lastWindowSeconds = windowSeconds;
            return nextDecision;
        }
    }
}
