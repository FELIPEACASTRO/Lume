package com.lume.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.compliance")
public class SecurityComplianceProperties {

    private boolean darkWebEnabled;

    public boolean isDarkWebEnabled() {
        return darkWebEnabled;
    }

    public void setDarkWebEnabled(boolean darkWebEnabled) {
        this.darkWebEnabled = darkWebEnabled;
    }
}
