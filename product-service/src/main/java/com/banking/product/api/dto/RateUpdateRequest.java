package com.banking.product.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateUpdateRequest {
    @NotNull
    @Positive
    private BigDecimal interestRate;
}
