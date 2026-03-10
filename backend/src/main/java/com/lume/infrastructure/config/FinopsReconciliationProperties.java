package com.lume.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "lume.finops.reconciliation")
public class FinopsReconciliationProperties {

    private boolean enabled;
    private String cron = "0 30 2 1 * *";
    private boolean autoFixCreditDrift;
    private int maxWorkspacesPerRun = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public boolean isAutoFixCreditDrift() {
        return autoFixCreditDrift;
    }

    public void setAutoFixCreditDrift(boolean autoFixCreditDrift) {
        this.autoFixCreditDrift = autoFixCreditDrift;
    }

    public int getMaxWorkspacesPerRun() {
        return maxWorkspacesPerRun;
    }

    public void setMaxWorkspacesPerRun(int maxWorkspacesPerRun) {
        this.maxWorkspacesPerRun = maxWorkspacesPerRun;
    }
}
