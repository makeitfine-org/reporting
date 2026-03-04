package com.banking.transaction.infrastructure.kafka.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionCreatedEvent {
    private String transactionId;
    private String clientId;
    private String productId;
    private String productType;
    private BigDecimal amount;
    private String currency;
    private Instant transactedAt;
}
