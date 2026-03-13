package com.lume.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 101)
public class MdcContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            MDC.put("traceId", UUID.randomUUID().toString());

            Object workspaceId = request.getAttribute("workspaceId");
            if (workspaceId != null) {
                MDC.put("workspaceId", String.valueOf(workspaceId));
            }

            Object userId = request.getAttribute("userId");
            if (userId != null) {
                MDC.put("userId", String.valueOf(userId));
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
