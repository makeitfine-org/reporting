package com.banking.transaction.api.dto;

import lombok.Data;

@Data
public class KycStatusResponse {
    private String customerId;
    private String clientId;
    private String kycStatus;
    private String riskTier;
}
