package com.banking.reporting.domain.model;

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
public class ReportSnapshot {

    private String id;
    private String clientId;
    private String reportType;
    private String period;
    private BigDecimal totalAmount;
    private long transactionCount;
    private Instant snapshotAt;
}
