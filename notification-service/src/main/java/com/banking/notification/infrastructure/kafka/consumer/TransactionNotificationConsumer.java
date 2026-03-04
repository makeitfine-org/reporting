package com.banking.notification.infrastructure.kafka.consumer;

import com.banking.notification.application.NotificationService;
import com.banking.notification.infrastructure.kafka.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notification.transaction-created", groupId = "notification-service")
    public void consume(TransactionCreatedEvent event) {
        try {
            log.info("Received transaction-created for clientId={} txId={}", event.getClientId(), event.getTransactionId());
            notificationService.processTransactionNotification(
                    event.getClientId(),
                    event.getTransactionId(),
                    event.getAmount().toPlainString(),
                    event.getCurrency()
            );
        } catch (Exception e) {
            log.error("Error processing transaction notification: {}", e.getMessage(), e);
            throw e;
        }
    }
}
