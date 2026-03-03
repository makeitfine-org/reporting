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
public class FinancialReportDto {

    private String clientId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant periodFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant periodTo;

    private BigDecimal totalAmount;
    private long totalTransactions;
    private long completedTransactions;
    private long reversedTransactions;
    private long chargebacks;
    private BigDecimal trendPercentage;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant generatedAt;

    private String cacheSource;
}
