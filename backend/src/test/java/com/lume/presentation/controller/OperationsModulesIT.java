package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.lume.workspace.dto.CreateByokConnectionRequest;
import com.lume.workspace.dto.CreateSupportTicketRequest;
import com.lume.workspace.dto.UpdateByokConnectionRequest;
import com.lume.workspace.dto.UpdateSupportTicketRequest;
import com.lume.workspace.dto.UpdateWorkspaceBudgetRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Support, FinOps anomalies and BYOK - Integration Tests")
class OperationsModulesIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST/GET/PATCH /api/v1/support/tickets - should open and resolve ticket")
    void shouldOpenAndResolveSupportTicket() throws Exception {
        CreateSupportTicketRequest createRequest = new CreateSupportTicketRequest(
                "Falha intermitente na conectividade de provider",
                "Provider retorna erro em parte das tentativas. Validar circuito e retries.",
                "provider",
                "high"
        );

        String createBody = mockMvc.perform(post("/v1/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.slaTargetAt").isString())
                .andExpect(jsonPath("$.slaBreached").isBoolean())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ticketId = JsonPath.read(createBody, "$.id");

        mockMvc.perform(get("/v1/support/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ticketId)));

        UpdateSupportTicketRequest updateRequest = new UpdateSupportTicketRequest(
                "resolved",
                "Retry policy ajustada e validada.",
                null,
                null
        );

        mockMvc.perform(patch("/v1/support/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.resolutionNote").value("Retry policy ajustada e validada."))
                .andExpect(jsonPath("$.slaTargetAt").value(nullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/finops/anomalies - should return budget anomaly when hard limit is reached")
    void shouldReturnFinopsAnomalies() throws Exception {
        String currentBudgetBody = mockMvc.perform(get("/v1/budgets/current"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String currentCostCenter = JsonPath.read(currentBudgetBody, "$.costCenter");
        String currentChargebackMode = JsonPath.read(currentBudgetBody, "$.chargebackMode");
        Integer currentSoftLimit = JsonPath.read(currentBudgetBody, "$.softLimitCredits");
        Integer currentHardLimit = JsonPath.read(currentBudgetBody, "$.hardLimitCredits");

        try {
            UpdateWorkspaceBudgetRequest budgetRequest = new UpdateWorkspaceBudgetRequest(
                    null,
                    null,
                    1,
                    1
            );

            mockMvc.perform(patch("/v1/budgets/current")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(budgetRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hardLimitCredits").value(1));

            mockMvc.perform(get("/v1/finops/anomalies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].code", hasItem("budget_hard_limit_reached")));
        } finally {
            UpdateWorkspaceBudgetRequest restoreRequest = new UpdateWorkspaceBudgetRequest(
                    currentCostCenter,
                    currentChargebackMode,
                    currentSoftLimit,
                    currentHardLimit
            );

            mockMvc.perform(patch("/v1/budgets/current")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(restoreRequest)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("POST/GET/PATCH /api/v1/byok/connections - should create and validate connection")
    void shouldCreateAndValidateByokConnection() throws Exception {
        CreateByokConnectionRequest createRequest = new CreateByokConnectionRequest(
                "openai",
                "byok-openai-test",
                "BILLING_WEBHOOK_SECRET",
                "workspace"
        );

        String createBody = mockMvc.perform(post("/v1/byok/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerCode").value("openai"))
                .andExpect(jsonPath("$.healthStatus").value("unknown"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String connectionId = JsonPath.read(createBody, "$.id");

        mockMvc.perform(post("/v1/byok/connections/{id}/validate", connectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("healthy"));

        mockMvc.perform(get("/v1/byok/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(connectionId)));

        UpdateByokConnectionRequest updateRequest = new UpdateByokConnectionRequest(
                null,
                null,
                "disabled",
                null
        );

        mockMvc.perform(patch("/v1/byok/connections/{id}", connectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("disabled"));
    }

    @Test
    @DisplayName("GET /api/v1/finops/scorecard - should expose operational billing and governance metrics")
    void shouldReturnFinopsScorecard() throws Exception {
        mockMvc.perform(get("/v1/finops/scorecard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyActiveUsers").isNumber())
                .andExpect(jsonPath("$.monthlyActiveUsers").isNumber())
                .andExpect(jsonPath("$.paidInvoicesCount").isNumber())
                .andExpect(jsonPath("$.paymentFailureCount").isNumber())
                .andExpect(jsonPath("$.orphanPaymentEvents").isNumber())
                .andExpect(jsonPath("$.pendingPaymentEvents").isNumber())
                .andExpect(jsonPath("$.openSupportTickets").isNumber())
                .andExpect(jsonPath("$.criticalOpenSupportTickets").isNumber())
                .andExpect(jsonPath("$.overdueSupportTickets").isNumber())
                .andExpect(jsonPath("$.byokConnections").isNumber())
                .andExpect(jsonPath("$.healthyByokConnections").isNumber())
                .andExpect(jsonPath("$.coreLiveProviders").isNumber())
                .andExpect(jsonPath("$.supportedRestrictedProviders").isNumber())
                .andExpect(jsonPath("$.blockedProviders").isNumber())
                .andExpect(jsonPath("$.notes").isString());
    }

    @Test
    @DisplayName("GET /api/v1/providers/readiness - should expose provider tier and blocker data")
    void shouldReturnProviderReadiness() throws Exception {
        mockMvc.perform(get("/v1/providers/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].providerCode").isString())
                .andExpect(jsonPath("$[0].providerTier").isString())
                .andExpect(jsonPath("$[0].readinessStatus").isString())
                .andExpect(jsonPath("$[0].smokeStatus").exists())
                .andExpect(jsonPath("$[0].missingCredentialEnvVars").isArray());
    }

    @Test
    @DisplayName("GET/PATCH /api/v1/settings/compliance - should persist workspace compliance controls")
    void shouldReadAndUpdateComplianceControls() throws Exception {
        mockMvc.perform(get("/v1/settings/compliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionPolicyStatus").value("not_configured"))
                .andExpect(jsonPath("$.accessReviewStatus").value("not_configured"));

        String payload = """
                {
                  "retentionPolicyStatus": "configured",
                  "retentionDays": 30,
                  "accessReviewStatus": "configured",
                  "accessReviewFrequencyDays": 45,
                  "consentTrackingEnabled": true,
                  "termsVersion": "v2026.03"
                }
                """;

        mockMvc.perform(patch("/v1/settings/compliance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionPolicyStatus").value("configured"))
                .andExpect(jsonPath("$.retentionDays").value(30))
                .andExpect(jsonPath("$.accessReviewStatus").value("configured"))
                .andExpect(jsonPath("$.accessReviewFrequencyDays").value(45))
                .andExpect(jsonPath("$.consentTrackingEnabled").value(true))
                .andExpect(jsonPath("$.termsVersion").value("v2026.03"));

        mockMvc.perform(get("/v1/settings/audit-feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("workspace_compliance"))
                .andExpect(jsonPath("$[0].action").value("updated"))
                .andExpect(jsonPath("$[0].payload").isString());
    }
}

