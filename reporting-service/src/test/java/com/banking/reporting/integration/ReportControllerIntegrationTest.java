package com.banking.reporting.integration;

import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import com.banking.reporting.infrastructure.postgres.repository.ReportConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportConfigRepository reportConfigRepository;

    @BeforeEach
    void setUp() {
        reportConfigRepository.deleteAll();
        ReportConfig config = ReportConfig.builder()
                .clientId("cli-001")
                .reportType("FINANCIAL")
                .currency("USD")
                .timezone("UTC")
                .alertsEnabled(false)
                .build();
        reportConfigRepository.save(config);
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getFinancialReport_validRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "cli-001")
                .param("period", "2022-01")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clientId").value("cli-001"));
    }

    @Test
    void getFinancialReport_noAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "cli-001")
                .param("period", "2022-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getFinancialReport_unknownClient_returns404() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "unknown-client")
                .param("period", "2022-01")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getFinancialReport_invalidPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                .param("clientId", "cli-001")
                .param("period", "invalid-period"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void getDashboard_validRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard")
                .param("clientId", "cli-001")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("cli-001"));
    }
}
