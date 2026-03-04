package com.banking.notification.infrastructure.kafka.event;

import lombok.Data;

import java.time.Instant;

@Data
public class CustomerUpdatedEvent {
    private String customerId;
    private String clientId;
    private String eventType;
    private String kycStatus;
    private String riskTier;
    private String email;
    private Instant occurredAt;
}
