package com.banking.reporting.unit;

import com.banking.reporting.api.dto.DashboardDto;
import com.banking.reporting.application.DashboardService;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.postgres.repository.AlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionProjectionRepository projectionRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboard_returnsDashboardWithCorrectTotals() {
        List<TransactionProjection> projections = List.of(
                TransactionProjection.builder()
                        .transactionId("tx-001")
                        .clientId("cli-001")
                        .status("COMPLETED")
                        .amount(BigDecimal.valueOf(500))
                        .transactedAt(Instant.now())
                        .build(),
                TransactionProjection.builder()
                        .transactionId("tx-002")
                        .clientId("cli-001")
                        .status("COMPLETED")
                        .amount(BigDecimal.valueOf(300))
                        .transactedAt(Instant.now())
                        .build()
        );

        when(projectionRepository.findByClientIdAndTransactedAtBetween(eq("cli-001"), any(), any()))
                .thenReturn(projections);
        when(alertRuleRepository.countByClientIdAndEnabled("cli-001", true)).thenReturn(2L);

        DashboardDto dashboard = dashboardService.getDashboard("cli-001");

        assertThat(dashboard.getClientId()).isEqualTo("cli-001");
        assertThat(dashboard.getTotalVolumeToday()).isEqualTo(BigDecimal.valueOf(800));
        assertThat(dashboard.getTransactionCountToday()).isEqualTo(2);
        assertThat(dashboard.getActiveAlerts()).isEqualTo(2);
        assertThat(dashboard.getRecentTransactions()).hasSize(2);
        assertThat(dashboard.getGeneratedAt()).isNotNull();
    }

    @Test
    void dashboardFallback_returnsEmptyDashboard() {
        DashboardDto fallback = dashboardService.dashboardFallback("cli-001", new RuntimeException("ES down"));

        assertThat(fallback.getClientId()).isEqualTo("cli-001");
        assertThat(fallback.getTotalVolumeToday()).isEqualTo(BigDecimal.ZERO);
        assertThat(fallback.getTransactionCountToday()).isEqualTo(0);
        assertThat(fallback.getRecentTransactions()).isEmpty();
    }
}
