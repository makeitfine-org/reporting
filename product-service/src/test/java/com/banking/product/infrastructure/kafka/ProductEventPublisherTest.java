package com.banking.product.infrastructure.kafka;

import com.banking.product.domain.ProductType;
import com.banking.product.infrastructure.postgres.entity.BankProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ProductEventPublisher eventPublisher;

    @Test
    void publishRateUpdated_success() {
        BankProduct product = BankProduct.builder()
                .productId("p1")
                .name("Product 1")
                .type(ProductType.SAVINGS)
                .interestRate(new BigDecimal("0.05"))
                .previousRate(new BigDecimal("0.04"))
                .build();

        eventPublisher.publishRateUpdated(product);

        verify(kafkaTemplate).send(eq("reporting.product-rate-updated"), eq("p1"), any());
    }
}
