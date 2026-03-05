package com.banking.transaction.unit;

import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.infrastructure.kafka.TransactionEventPublisher;
import com.banking.transaction.infrastructure.postgres.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionEventPublisher eventPublisher;

    @Test
    void publishTransactionCreated_sendsToTwoTopics() {
        Transaction tx = Transaction.builder()
                .id("tx-pub-1").clientId("c-1").productId("p-1")
                .productType("MORTGAGE").amount(new BigDecimal("5000"))
                .currency("EUR").status(TransactionStatus.COMPLETED).build();

        eventPublisher.publishTransactionCreated(tx);

        verify(kafkaTemplate).send(
                eq("reporting.transaction-created"), eq("c-1"), any());
        verify(kafkaTemplate).send(
                eq("notification.transaction-created"), eq("c-1"), any());
    }

    @Test
    void publishTransactionReversed_sendsToReportingTopic() {
        Transaction tx = Transaction.builder()
                .id("tx-pub-2").clientId("c-2").productId("p-2")
                .productType("LOAN").amount(new BigDecimal("1000"))
                .currency("USD").status(TransactionStatus.REVERSED).build();

        eventPublisher.publishTransactionReversed(tx);

        verify(kafkaTemplate, times(1)).send(
                eq("reporting.transaction-reversed"), eq("c-2"), any());
    }
}
