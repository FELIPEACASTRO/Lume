package com.lume.infrastructure.web;

import com.lume.infrastructure.config.WorkspaceAuthProperties;
import com.lume.workspace.service.WorkspaceSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Authentication filter that rejects unauthenticated requests before they reach controllers.
 *
 * <p>Public paths (auth, setup, health, actuator, swagger) are allowed through without
 * session validation. All other requests must have a valid session cookie.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class LumeAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/v1/auth/",
            "/v1/setup/",
            "/health",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui"
    );

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/v1/auth/login",
            "/v1/auth/logout",
            "/v1/auth/session",
            "/v1/setup/status",
            "/v1/setup/bootstrap"
    );

    private final WorkspaceSessionService workspaceSessionService;
    private final WorkspaceAuthProperties workspaceAuthProperties;

    public LumeAuthenticationFilter(
            WorkspaceSessionService workspaceSessionService,
            WorkspaceAuthProperties workspaceAuthProperties
    ) {
        this.workspaceSessionService = workspaceSessionService;
        this.workspaceAuthProperties = workspaceAuthProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isNonProd = !workspaceAuthProperties.isProductionProfile();

        if (isNonProd && workspaceAuthProperties.isAllowTestHeader() && hasTestActorHeader(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isNonProd && workspaceAuthProperties.isAllowTestAutoLogin()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (workspaceSessionService.resolveAuthenticatedUser(request).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":401,\"message\":\"A sessao atual nao esta autenticada.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getServletPath();

        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }

        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return false;
    }

    private boolean hasTestActorHeader(HttpServletRequest request) {
        String actorHeader = request.getHeader("X-Lume-Actor-User-Id");
        return actorHeader != null && !actorHeader.isBlank();
    }
}
