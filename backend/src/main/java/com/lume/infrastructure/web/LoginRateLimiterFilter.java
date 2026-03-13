package com.lume.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Centralized rate limiter for the login endpoint.
 * Tracks attempts in persistent storage using IP + identity when available.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class LoginRateLimiterFilter extends OncePerRequestFilter {

    private final LoginRateLimitService loginRateLimitService;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final int windowSeconds;

    public LoginRateLimiterFilter(
            LoginRateLimitService loginRateLimitService,
            ObjectMapper objectMapper,
            @Value("${lume.rate-limit.login.max-attempts:10}") int maxAttempts,
            @Value("${lume.rate-limit.login.window-seconds:60}") int windowSeconds) {
        this.loginRateLimitService = loginRateLimitService;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/v1/auth/login".equals(resolvePath(request))
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String clientIp = resolveClientIp(cachedRequest);
        String identity = resolveLoginIdentity(cachedRequest);
        LoginRateLimitService.LoginRateLimitDecision decision = loginRateLimitService.registerAttempt(
                clientIp,
                identity,
                maxAttempts,
                windowSeconds
        );

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingAttempts()));

        if (decision.blocked()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.getWriter().write("{\"status\":429,\"message\":\"Muitas tentativas de login. Tente novamente em " + decision.retryAfterSeconds() + " segundos.\"}");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }

        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "";
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private String resolveLoginIdentity(CachedBodyHttpServletRequest request) {
        byte[] body = request.getCachedBody();
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode payload = objectMapper.readTree(body);
            if (payload == null) {
                return null;
            }
            JsonNode emailNode = payload.path("email");
            if (emailNode.isTextual()) {
                String email = emailNode.asText().trim();
                return email.isBlank() ? null : email.toLowerCase();
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
