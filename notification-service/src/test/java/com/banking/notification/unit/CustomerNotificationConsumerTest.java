package com.banking.notification.unit;

import com.banking.notification.application.NotificationService;
import com.banking.notification.infrastructure.kafka.consumer.CustomerNotificationConsumer;
import com.banking.notification.infrastructure.kafka.event.CustomerUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerNotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CustomerNotificationConsumer consumer;

    @Test
    void consume_validEvent_delegatesToNotificationService() {
        CustomerUpdatedEvent event = new CustomerUpdatedEvent();
        event.setClientId("cli-001");
        event.setEventType("KYC_APPROVED");
        event.setOccurredAt(Instant.now());

        consumer.consume(event);

        verify(notificationService).processCustomerNotification(eq("cli-001"), eq("KYC_APPROVED"));
    }

    @Test
    void consume_serviceThrows_rethrowsException() {
        CustomerUpdatedEvent event = new CustomerUpdatedEvent();
        event.setClientId("cli-002");
        event.setEventType("RISK_UPDATED");
        event.setOccurredAt(Instant.now());

        doThrow(new RuntimeException("DB unavailable"))
                .when(notificationService).processCustomerNotification(any(), any());

        assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB unavailable");
    }

    @Test
    void consume_invalidPayload_rethrowsException() {
        doThrow(new IllegalArgumentException("Cannot deserialize"))
                .when(objectMapper).convertValue(any(), eq(CustomerUpdatedEvent.class));

        assertThatThrownBy(() -> consumer.consume(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot deserialize");
    }
}
