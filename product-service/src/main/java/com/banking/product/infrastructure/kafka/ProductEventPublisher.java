package com.banking.product.infrastructure.kafka;

import com.banking.product.infrastructure.kafka.event.ProductRateUpdatedEvent;
import com.banking.product.infrastructure.postgres.entity.BankProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private static final String TOPIC = "reporting.product-rate-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRateUpdated(BankProduct product) {
        ProductRateUpdatedEvent event = ProductRateUpdatedEvent.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .type(product.getType())
                .previousRate(product.getPreviousRate())
                .newRate(product.getInterestRate())
                .occurredAt(Instant.now())
                .build();
        kafkaTemplate.send(TOPIC, product.getProductId(), event);
        log.info("Published rate-updated for productId={} newRate={}", product.getProductId(), product.getInterestRate());
    }
}
