package com.lume.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "lume.auth.allow-test-auto-login=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Authentication Filter - Integration Tests")
class AuthenticationFilterIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/agents/profiles without auth header should return 401 JSON")
    void shouldReturn401ForProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/v1/agents/profiles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/agents/profiles with X-Lume-Actor-User-Id header should return 200")
    void shouldAllowProtectedEndpointWithTestActorHeader() throws Exception {
        mockMvc.perform(get("/v1/agents/profiles")
                        .header("X-Lume-Actor-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auth/session without auth header should return 401 when auto-login is disabled")
    void shouldReturn401ForSessionEndpointWithoutAuthWhenAutoLoginIsDisabled() throws Exception {
        mockMvc.perform(get("/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("OPTIONS /api/v1/agents/profiles should not return 401")
    void shouldAllowOptionsOnProtectedEndpoint() throws Exception {
        int status = mockMvc.perform(options("/v1/agents/profiles"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login 11 times should return 429 on the last attempt")
    void shouldReturn429AfterExcessiveLoginAttempts() throws Exception {
        String loginBody = "{\"email\":\"attacker@example.com\",\"password\":\"wrong\"}";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody));
        }

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").exists());
    }
}

