package com.banking.reporting.infrastructure.kafka.consumer;

import com.banking.reporting.infrastructure.elasticsearch.document.ProductDetails;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.event.ProductRateUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final TransactionProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "reporting.product-rate-updated",
        groupId = "reporting-service",
        containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleProductRateUpdated(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            ProductRateUpdatedEvent event = objectMapper.convertValue(payload, ProductRateUpdatedEvent.class);
            log.info("Processing ProductRateUpdated: productId={} clientId={} newRate={}",
                    event.getProductId(), event.getClientId(), event.getNewRate());

            // Update all projections for the affected client that reference this product
            var projections = projectionRepository.findByClientId(
                    event.getClientId(), PageRequest.of(0, 1000));

            projections.stream()
                    .filter(p -> p.getProductDetails() != null
                            && event.getProductId().equals(p.getProductDetails().getProductId()))
                    .forEach(projection -> {
                        ProductDetails updatedDetails = ProductDetails.builder()
                                .productId(projection.getProductDetails().getProductId())
                                .productName(projection.getProductDetails().getProductName())
                                .amount(projection.getProductDetails().getAmount())
                                .interestRate(event.getNewRate())
                                .build();
                        projection.setProductDetails(updatedDetails);
                        projectionRepository.save(projection);
                    });

            log.debug("Updated rate for productId={} on client projections", event.getProductId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ProductRateUpdated event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
