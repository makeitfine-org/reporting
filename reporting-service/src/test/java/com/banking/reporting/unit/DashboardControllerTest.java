package com.banking.reporting.unit;

import com.banking.reporting.api.DashboardController;
import com.banking.reporting.api.dto.DashboardDto;
import com.banking.reporting.application.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getDashboard_returnsDashboardDto() throws Exception {
        DashboardDto dashboardDto = DashboardDto.builder()
                .clientId("cli-001")
                .transactionCountToday(100)
                .build();

        when(dashboardService.getDashboard(anyString())).thenReturn(dashboardDto);

        mockMvc.perform(get("/api/reports/dashboard")
                        .param("clientId", "cli-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("cli-001"))
                .andExpect(jsonPath("$.transactionCountToday").value(100));
    }

    @Test
    void getDashboard_withEmptyClientId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard")
                        .param("clientId", ""))
                .andExpect(status().isBadRequest());
    }
}
