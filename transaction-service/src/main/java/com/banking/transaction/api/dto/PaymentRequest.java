package com.banking.transaction.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotBlank
    private String clientId;
    @NotBlank
    private String productId;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String currency;
}
