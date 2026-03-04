package com.banking.customer.api.dto;

import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycStatusResponse {
    private String customerId;
    private String clientId;
    private KycStatus kycStatus;
    private RiskTier riskTier;
}
