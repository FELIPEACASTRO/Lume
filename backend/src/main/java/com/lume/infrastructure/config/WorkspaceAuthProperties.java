package com.lume.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lume.auth")
public class WorkspaceAuthProperties {

    private boolean allowTestHeader;
    private boolean allowTestAutoLogin;
    private String sessionCookieName = "LUME_SESSION";
    private int sessionDurationHours = 12;

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
