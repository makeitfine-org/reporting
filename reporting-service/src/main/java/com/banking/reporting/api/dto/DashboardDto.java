package com.banking.reporting.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {

    private String clientId;
    private BigDecimal totalVolumeToday;
    private long transactionCountToday;
    private long activeAlerts;
    private BigDecimal revenueThisMonth;
    private List<TransactionSummaryDto> recentTransactions;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant generatedAt;
}
