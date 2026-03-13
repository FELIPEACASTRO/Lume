package com.lume.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Configuration Properties - Integration Tests")
class ConfigurationPropertiesIT {

    @Autowired
    private SlaProperties slaProperties;

    @Autowired
    private FinopsAnomalyProperties finopsAnomalyProperties;

    @Autowired
    private WorkspaceAuthProperties workspaceAuthProperties;

    @Test
    @DisplayName("Should bind SLA properties with default values")
    void shouldBindSlaProperties() {
        assertThat(slaProperties.getCriticalMinutes()).isEqualTo(15);
        assertThat(slaProperties.getHighMinutes()).isEqualTo(60);
        assertThat(slaProperties.getMediumMinutes()).isEqualTo(240);
        assertThat(slaProperties.getLowMinutes()).isEqualTo(1440);
    }

    @Test
    @DisplayName("Should bind FinOps anomaly properties with default values")
    void shouldBindFinopsAnomalyProperties() {
        assertThat(finopsAnomalyProperties.getCostThreshold()).isEqualTo(0.08);
        assertThat(finopsAnomalyProperties.getSuccessRateCritical()).isEqualTo(70.0);
        assertThat(finopsAnomalyProperties.getSuccessRateWarning()).isEqualTo(85.0);
    }

    @Test
    @DisplayName("Should bind auth properties with test profile overrides")
    void shouldBindAuthProperties() {
        assertThat(workspaceAuthProperties.isAllowTestHeader()).isTrue();
    }
}
