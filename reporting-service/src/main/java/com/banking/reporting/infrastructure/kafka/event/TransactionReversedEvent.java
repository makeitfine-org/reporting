package com.banking.reporting.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionReversedEvent {

    private String eventType;
    private String schemaVersion;
    private String transactionId;
    private String clientId;
    private String correlationId;
    private String reason;
    private Instant occurredAt;
}
