package com.banking.reporting.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDto {

    private String clientId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant periodFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant periodTo;

    private BigDecimal totalRevenue;
    private int transactionCount;
    private BigDecimal averageTransactionValue;
    private BigDecimal previousPeriodRevenue;
    private BigDecimal growthRate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant generatedAt;

    private String cacheSource;
}
