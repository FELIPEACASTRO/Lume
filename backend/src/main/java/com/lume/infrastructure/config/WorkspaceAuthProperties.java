package com.lume.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@ConfigurationProperties(prefix = "lume.auth")
public class WorkspaceAuthProperties {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAuthProperties.class);

    private final Environment environment;

    private boolean allowTestHeader;
    private boolean allowTestAutoLogin;
    private String sessionCookieName = "LUME_SESSION";
    private int sessionDurationHours = 12;

    public WorkspaceAuthProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateTestFlagsAgainstProfile() {
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        if (isProd && (allowTestHeader || allowTestAutoLogin)) {
            log.error("SECURITY: Test auth flags (allowTestHeader={}, allowTestAutoLogin={}) "
                    + "are enabled in production profile. These flags will be IGNORED at runtime.", allowTestHeader, allowTestAutoLogin);
        }
    }

    public boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

    public boolean isAllowTestHeader() {
        return allowTestHeader;
    }

    public void setAllowTestHeader(boolean allowTestHeader) {
        this.allowTestHeader = allowTestHeader;
    }

    public boolean isAllowTestAutoLogin() {
        return allowTestAutoLogin;
    }

    public void setAllowTestAutoLogin(boolean allowTestAutoLogin) {
        this.allowTestAutoLogin = allowTestAutoLogin;
    }

    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public int getSessionDurationHours() {
        return sessionDurationHours;
    }

    public void setSessionDurationHours(int sessionDurationHours) {
        this.sessionDurationHours = sessionDurationHours;
    }
}
