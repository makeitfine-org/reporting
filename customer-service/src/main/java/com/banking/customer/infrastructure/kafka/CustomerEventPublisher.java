package com.banking.customer.infrastructure.kafka;

import com.banking.customer.infrastructure.kafka.event.CustomerUpdatedEvent;
import com.banking.customer.infrastructure.postgres.entity.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private static final String TOPIC = "notification.customer-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCustomerUpdated(Customer customer, String eventType) {
        CustomerUpdatedEvent event = CustomerUpdatedEvent.builder()
                .customerId(customer.getId())
                .clientId(customer.getClientId())
                .eventType(eventType)
                .kycStatus(customer.getKycStatus())
                .riskTier(customer.getRiskTier())
                .email(customer.getEmail())
                .occurredAt(Instant.now())
                .build();
        kafkaTemplate.send(TOPIC, customer.getClientId(), event);
        log.info("Published {} event for clientId={}", eventType, customer.getClientId());
    }
}
