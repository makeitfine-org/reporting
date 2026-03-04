package com.banking.notification.infrastructure.kafka.event;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class TransactionCreatedEvent {
    private String transactionId;
    private String clientId;
    private String productId;
    private String productType;
    private BigDecimal amount;
    private String currency;
    private Instant transactedAt;
}
