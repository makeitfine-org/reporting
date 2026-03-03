package com.banking.reporting.unit;

import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.consumer.TransactionEventConsumer;
import com.banking.reporting.infrastructure.kafka.event.TransactionCreatedEvent;
import com.banking.reporting.infrastructure.kafka.event.TransactionReversedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private TransactionProjectionRepository projectionRepository;

    @Mock
    private Acknowledgment acknowledgment;

    private TransactionEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        consumer = new TransactionEventConsumer(projectionRepository, objectMapper);
    }

    @Test
    void handleTransactionCreated_savesProjectionAndAcknowledges() {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId("tx-001")
                .clientId("cli-001")
                .amount(BigDecimal.valueOf(1000))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        consumer.handleTransactionCreated(event, "reporting.transaction-created", "cli-001", acknowledgment);

        ArgumentCaptor<TransactionProjection> captor = ArgumentCaptor.forClass(TransactionProjection.class);
        verify(projectionRepository).save(captor.capture());
        verify(acknowledgment).acknowledge();

        TransactionProjection saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo("tx-001");
        assertThat(saved.getClientId()).isEqualTo("cli-001");
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    void handleTransactionReversed_updatesProjectionStatus() {
        TransactionProjection existingProjection = TransactionProjection.builder()
                .transactionId("tx-001")
                .clientId("cli-001")
                .status("COMPLETED")
                .build();

        when(projectionRepository.findById("tx-001")).thenReturn(Optional.of(existingProjection));

        TransactionReversedEvent event = TransactionReversedEvent.builder()
                .transactionId("tx-001")
                .clientId("cli-001")
                .reason("Duplicate")
                .occurredAt(Instant.now())
                .build();

        consumer.handleTransactionReversed(event, "reporting.transaction-reversed", acknowledgment);

        verify(projectionRepository).save(existingProjection);
        assertThat(existingProjection.getStatus()).isEqualTo("REVERSED");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleTransactionReversed_projectionNotFound_logsWarningAndAcknowledges() {
        when(projectionRepository.findById("tx-999")).thenReturn(Optional.empty());

        TransactionReversedEvent event = TransactionReversedEvent.builder()
                .transactionId("tx-999")
                .clientId("cli-001")
                .reason("Test")
                .occurredAt(Instant.now())
                .build();

        consumer.handleTransactionReversed(event, "reporting.transaction-reversed", acknowledgment);

        verify(projectionRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
