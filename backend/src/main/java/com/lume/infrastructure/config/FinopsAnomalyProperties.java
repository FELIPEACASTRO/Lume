package com.lume.infrastructure.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "lume.finops.anomaly")
public class FinopsAnomalyProperties {

    @Positive private double costThreshold = 0.08;
    @Positive private double successRateCritical = 70.0;
    @Positive private double successRateWarning = 85.0;

    public double getCostThreshold() {
        return costThreshold;
    }

    public void setCostThreshold(double costThreshold) {
        this.costThreshold = costThreshold;
    }

    public double getSuccessRateCritical() {
        return successRateCritical;
    }

    public void setSuccessRateCritical(double successRateCritical) {
        this.successRateCritical = successRateCritical;
    }

    public double getSuccessRateWarning() {
        return successRateWarning;
    }

    public void setSuccessRateWarning(double successRateWarning) {
        this.successRateWarning = successRateWarning;
    }
}
