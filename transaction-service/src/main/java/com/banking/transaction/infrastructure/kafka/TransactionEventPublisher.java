package com.banking.transaction.infrastructure.kafka;

import com.banking.transaction.infrastructure.kafka.event.TransactionCreatedEvent;
import com.banking.transaction.infrastructure.kafka.event.TransactionReversedEvent;
import com.banking.transaction.infrastructure.postgres.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionCreated(Transaction tx) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(tx.getId())
                .clientId(tx.getClientId())
                .productId(tx.getProductId())
                .productType(tx.getProductType())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .transactedAt(tx.getTransactedAt())
                .build();
        kafkaTemplate.send("reporting.transaction-created", tx.getClientId(), event);
        kafkaTemplate.send("notification.transaction-created", tx.getClientId(), event);
        log.info("Published transaction-created txId={}", tx.getId());
    }

    public void publishTransactionReversed(Transaction tx) {
        TransactionReversedEvent event = TransactionReversedEvent.builder()
                .transactionId(tx.getId())
                .clientId(tx.getClientId())
                .reversedAt(Instant.now())
                .build();
        kafkaTemplate.send("reporting.transaction-reversed", tx.getClientId(), event);
        log.info("Published transaction-reversed txId={}", tx.getId());
    }
}
