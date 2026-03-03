package com.banking.reporting.unit;

import com.banking.reporting.api.ReportController;
import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.application.ReportQueryService;
import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import com.banking.reporting.infrastructure.postgres.repository.ReportConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportQueryService reportQueryService;

    @MockitoBean
    private ReportConfigRepository reportConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getFinancialReport_returnsFinancialReportDto() throws Exception {
        FinancialReportDto report = FinancialReportDto.builder()
                .clientId("cli-001")
                .build();

        when(reportQueryService.getFinancialReport(anyString(), anyString())).thenReturn(report);

        mockMvc.perform(get("/api/reports/financial")
                        .param("clientId", "cli-001")
                        .param("period", "2022-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("cli-001"));
    }

    @Test
    void getFinancialReport_withInvalidPeriod_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/financial")
                        .param("clientId", "cli-001")
                        .param("period", "invalid-period"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRevenueReport_returnsRevenueReportDto() throws Exception {
        RevenueReportDto report = RevenueReportDto.builder()
                .clientId("cli-001")
                .build();

        when(reportQueryService.getRevenueReport(anyString(), anyString())).thenReturn(report);

        mockMvc.perform(get("/api/reports/revenue")
                        .param("clientId", "cli-001")
                        .param("period", "2022-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("cli-001"));
    }

    @Test
    void getTransactions_returnsList() throws Exception {
        when(reportQueryService.getTransactions(anyString(), anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reports/transactions")
                        .param("clientId", "cli-001")
                        .param("period", "2022-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createReportConfig_updatesExistingConfig() throws Exception {
        ReportConfig existing = new ReportConfig();
        existing.setClientId("cli-001");
        existing.setCurrency("EUR");

        when(reportConfigRepository.findByClientId("cli-001")).thenReturn(Optional.of(existing));
        when(reportConfigRepository.save(any(ReportConfig.class))).thenReturn(existing);

        String jsonRequest = """
                {
                  "clientId": "cli-001",
                  "reportType": "MONTHLY",
                  "currency": "USD",
                  "timezone": "UTC",
                  "alertsEnabled": true
                }
                """;

        mockMvc.perform(post("/api/reports/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());

        verify(reportConfigRepository).save(argThat(config -> "USD".equals(config.getCurrency())));
    }
}
