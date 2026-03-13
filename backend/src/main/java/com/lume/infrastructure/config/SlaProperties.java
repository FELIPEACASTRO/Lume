package com.lume.infrastructure.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "lume.sla")
public class SlaProperties {

    @Positive private int criticalMinutes = 15;
    @Positive private int highMinutes = 60;
    @Positive private int mediumMinutes = 240;
    @Positive private int lowMinutes = 1440;

    public int getCriticalMinutes() {
        return criticalMinutes;
    }

    public void setCriticalMinutes(int criticalMinutes) {
        this.criticalMinutes = criticalMinutes;
    }

    public int getHighMinutes() {
        return highMinutes;
    }

    public void setHighMinutes(int highMinutes) {
        this.highMinutes = highMinutes;
    }

    public int getMediumMinutes() {
        return mediumMinutes;
    }

    public void setMediumMinutes(int mediumMinutes) {
        this.mediumMinutes = mediumMinutes;
    }

    public int getLowMinutes() {
        return lowMinutes;
    }

    public void setLowMinutes(int lowMinutes) {
        this.lowMinutes = lowMinutes;
    }
}
