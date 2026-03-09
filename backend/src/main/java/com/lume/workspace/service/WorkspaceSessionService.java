package com.lume.workspace.service;

import com.lume.infrastructure.config.WorkspaceAuthProperties;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.entity.AuthSessionJpaEntity;
import com.lume.workspace.repository.AuthSessionJpaRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceSessionService {

    private final AuthSessionJpaRepository authSessionRepository;
    private final JpaUserRepository userRepository;
    private final WorkspaceAuthProperties workspaceAuthProperties;

    public WorkspaceSessionService(
            AuthSessionJpaRepository authSessionRepository,
            JpaUserRepository userRepository,
            WorkspaceAuthProperties workspaceAuthProperties
    ) {
        this.authSessionRepository = authSessionRepository;
        this.userRepository = userRepository;
        this.workspaceAuthProperties = workspaceAuthProperties;
    }

    @Transactional
    public ResponseCookie createSession(UserJpaEntity user, HttpServletRequest request) {
        purgeExpiredSessions();

        AuthSessionJpaEntity session = new AuthSessionJpaEntity();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setUserAgent(trimmed(request == null ? null : request.getHeader(HttpHeaders.USER_AGENT), 255));
        session.setIpAddress(trimmed(resolveRemoteAddress(request), 64));
        session.setExpiresAt(LocalDateTime.now().plusHours(workspaceAuthProperties.getSessionDurationHours()));
        authSessionRepository.save(session);

        return buildCookie(session.getId(), workspaceAuthProperties.getSessionDurationHours() * 60 * 60);
    }

    @Transactional
    public ResponseCookie invalidateSession(HttpServletRequest request) {
        sessionIdFromRequest(request)
                .flatMap(authSessionRepository::findByIdAndInvalidatedAtIsNull)
                .ifPresent(session -> {
                    session.setInvalidatedAt(LocalDateTime.now());
                    authSessionRepository.save(session);
                });

        return buildCookie("", 0);
    }

    @Transactional(readOnly = true)
    public Optional<UserJpaEntity> resolveAuthenticatedUser(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        return sessionIdFromRequest(request)
                .flatMap(authSessionRepository::findByIdAndInvalidatedAtIsNull)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .flatMap(session -> userRepository.findByIdAndActiveTrue(session.getUserId()));
    }

    @Transactional
    public void purgeExpiredSessions() {
        authSessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private Optional<String> sessionIdFromRequest(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> workspaceAuthProperties.getSessionCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie buildCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from(workspaceAuthProperties.getSessionCookieName(), value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String resolveRemoteAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trimmed(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }
}
