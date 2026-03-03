package com.banking.reporting.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "cli-001")
                .param("period", "2022-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void wrongRole_returns403() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "cli-001")
                .param("period", "2022-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void correctRole_proceedsToHandler() throws Exception {
        // Request with a valid role should pass security (will fail with 404 since no config exists for unknown client)
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "unknown")
                .param("period", "2022-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actuatorHealth_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
