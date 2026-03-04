package com.banking.customer.api.dto;

import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycUpdateRequest {
    @NotNull
    private KycStatus kycStatus;
    @NotNull
    private RiskTier riskTier;
}
