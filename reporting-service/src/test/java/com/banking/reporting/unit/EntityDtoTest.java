package com.banking.reporting.unit;

import com.banking.reporting.api.dto.*;
import com.banking.reporting.infrastructure.postgres.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EntityDtoTest {

    @Test
    void testEntities() {
        AlertRule ar = AlertRule.builder()
                .clientId("cli-1")
                .ruleType("THRESHOLD")
                .thresholdValue(BigDecimal.TEN)
                .enabled(true)
                .build();
        assertThat(ar.getClientId()).isEqualTo("cli-1");
        assertThat(ar.getThresholdValue()).isEqualTo(BigDecimal.TEN);
        assertThat(ar.isEnabled()).isTrue();

        ReportConfig rc = ReportConfig.builder()
                .clientId("cli-1")
                .currency("USD")
                .build();
        assertThat(rc.getClientId()).isEqualTo("cli-1");

        ScheduledReport sr = ScheduledReport.builder()
                .clientId("cli-1")
                .cronExpression("0 0 * * *")
                .build();
        assertThat(sr.getCronExpression()).isEqualTo("0 0 * * *");
    }

    @Test
    void testDtos() {
        DashboardDto d = DashboardDto.builder()
                .clientId("c")
                .totalVolumeToday(BigDecimal.ONE)
                .build();
        assertThat(d.getClientId()).isEqualTo("c");

        FinancialReportDto f = FinancialReportDto.builder()
                .clientId("c")
                .totalAmount(BigDecimal.ONE)
                .build();
        assertThat(f.getClientId()).isEqualTo("c");

        ProductSummaryDto p = ProductSummaryDto.builder()
                .productId("p")
                .build();
        assertThat(p.getProductId()).isEqualTo("p");

        ReportConfigRequest rcr = new ReportConfigRequest();
        rcr.setClientId("c");
        rcr.setAlertsEnabled(true);
        assertThat(rcr.getClientId()).isEqualTo("c");
        assertThat(rcr.isAlertsEnabled()).isTrue();

        RevenueReportDto rr = RevenueReportDto.builder()
                .totalRevenue(BigDecimal.ONE)
                .build();
        assertThat(rr.getTotalRevenue()).isEqualTo(BigDecimal.ONE);

        TransactionSummaryDto ts = TransactionSummaryDto.builder()
                .transactionId("t")
                .build();
        assertThat(ts.getTransactionId()).isEqualTo("t");
    }
}
