package com.banking.transaction.infrastructure.kafka.event;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TransactionReversedEvent {
    private String transactionId;
    private String clientId;
    private Instant reversedAt;
}
