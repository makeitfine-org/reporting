package com.banking.customer.api.dto;

import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CustomerResponse {
    private String id;
    private String clientId;
    private String name;
    private String email;
    private String phone;
    private KycStatus kycStatus;
    private RiskTier riskTier;
    private String region;
    private Instant createdAt;
    private Instant updatedAt;
}
