package com.banking.reporting.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCreatedEvent {

    private String eventType;
    private String schemaVersion;
    private String transactionId;
    private String clientId;
    private String correlationId;
    private BigDecimal amount;
    private String currency;
    private String productId;
    private String productType;
    private Instant occurredAt;
}
