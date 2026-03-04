package com.banking.customer.infrastructure.kafka.event;

import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CustomerUpdatedEvent {
    private String customerId;
    private String clientId;
    private String eventType; // PROFILE_UPDATED or KYC_UPDATED
    private KycStatus kycStatus;
    private RiskTier riskTier;
    private String email;
    private Instant occurredAt;
}
