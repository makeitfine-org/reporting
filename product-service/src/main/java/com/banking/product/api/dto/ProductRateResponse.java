package com.banking.product.api.dto;

import com.banking.product.domain.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductRateResponse {
    private String productId;
    private ProductType type;
    private BigDecimal interestRate;
    private boolean active;
}
