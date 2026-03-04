package com.banking.transaction.api.dto;

import com.banking.transaction.domain.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {
    private String id;
    private String clientId;
    private String productId;
    private String productType;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private Instant transactedAt;
}
