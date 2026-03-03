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
public class TransactionSummaryDto {

    private String transactionId;
    private String clientId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant transactedAt;

    private ProductSummaryDto productDetails;
    private String region;
}
