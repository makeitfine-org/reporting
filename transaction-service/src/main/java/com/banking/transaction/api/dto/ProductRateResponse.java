package com.banking.transaction.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRateResponse {
    private String productId;
    private String type;
    private BigDecimal interestRate;
    private boolean active;
}
