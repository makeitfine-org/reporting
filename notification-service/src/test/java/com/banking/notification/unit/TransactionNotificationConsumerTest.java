package com.banking.notification.unit;

import com.banking.notification.application.NotificationService;
import com.banking.notification.infrastructure.kafka.consumer.TransactionNotificationConsumer;
import com.banking.notification.infrastructure.kafka.event.TransactionCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionNotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @InjectMocks
    private TransactionNotificationConsumer consumer;

    @Test
    void consume_validEvent_delegatesToNotificationService() {
        TransactionCreatedEvent event = new TransactionCreatedEvent();
        event.setTransactionId("tx-001");
        event.setClientId("cli-001");
        event.setAmount(new BigDecimal("5000"));
        event.setCurrency("EUR");
        event.setTransactedAt(Instant.now());

        consumer.consume(event);

        verify(notificationService).processTransactionNotification(
                eq("cli-001"), eq("tx-001"), eq("5000"), eq("EUR"));
    }
}
