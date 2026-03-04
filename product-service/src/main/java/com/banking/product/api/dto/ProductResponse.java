package com.banking.product.api.dto;

import com.banking.product.domain.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String productId;
    private String name;
    private ProductType type;
    private BigDecimal interestRate;
    private BigDecimal previousRate;
    private boolean active;
    private Instant effectiveDate;
    private Instant createdAt;
    private Instant updatedAt;
}
