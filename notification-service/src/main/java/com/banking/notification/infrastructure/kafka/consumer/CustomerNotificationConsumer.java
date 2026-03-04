package com.banking.notification.infrastructure.kafka.consumer;

import com.banking.notification.application.NotificationService;
import com.banking.notification.infrastructure.kafka.event.CustomerUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification.customer-updated", groupId = "notification-service")
    public void consume(Object payload) {
        try {
            CustomerUpdatedEvent event = objectMapper.convertValue(payload, CustomerUpdatedEvent.class);
            log.info("Received customer-updated for clientId={} type={}", event.getClientId(), event.getEventType());
            notificationService.processCustomerNotification(event.getClientId(), event.getEventType());
        } catch (Exception e) {
            log.error("Error processing customer notification: {}", e.getMessage(), e);
            throw e;
        }
    }
}
