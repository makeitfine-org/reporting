package com.banking.product.infrastructure.kafka.event;

import com.banking.product.domain.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductRateUpdatedEvent {
    private String productId;
    private String name;
    private ProductType type;
    private BigDecimal previousRate;
    private BigDecimal newRate;
    private Instant occurredAt;
}
