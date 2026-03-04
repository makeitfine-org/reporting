package com.banking.product.api.dto;

import com.banking.product.domain.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank
    private String productId;
    @NotBlank
    private String name;
    @NotNull
    private ProductType type;
    @NotNull
    @Positive
    private BigDecimal interestRate;
}
