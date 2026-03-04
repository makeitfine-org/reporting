package com.banking.reporting.infrastructure.kafka.consumer;

import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.event.ChargebackProcessedEvent;
import com.banking.reporting.infrastructure.kafka.event.TransactionCreatedEvent;
import com.banking.reporting.infrastructure.kafka.event.TransactionReversedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionProjectionRepository projectionRepository;

    @KafkaListener(
            topics = "reporting.transaction-created",
            groupId = "reporting-service",
            containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleTransactionCreated(
            @Payload TransactionCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing TransactionCreated: transactionId={} clientId={} correlationId={}",
                    event.getTransactionId(), event.getClientId(), event.getCorrelationId());

            TransactionProjection projection = TransactionProjection.builder()
                    .transactionId(event.getTransactionId())
                    .clientId(event.getClientId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .status("COMPLETED")
                    .transactedAt(event.getOccurredAt())
                    .build();

            projectionRepository.save(projection);
            log.debug("Saved projection for transactionId={}", event.getTransactionId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing TransactionCreated event from topic={}: {}", topic, e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "reporting.transaction-reversed",
            groupId = "reporting-service",
            containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleTransactionReversed(
            @Payload TransactionReversedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing TransactionReversed: transactionId={} clientId={} reason={}",
                    event.getTransactionId(), event.getClientId(), event.getReason());

            projectionRepository.findById(event.getTransactionId()).ifPresentOrElse(
                    projection -> {
                        projection.setStatus("REVERSED");
                        projectionRepository.save(projection);
                        log.debug("Marked transaction {} as REVERSED", event.getTransactionId());
                    },
                    () -> log.warn("Projection not found for reversed transactionId={}", event.getTransactionId())
            );
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing TransactionReversed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "reporting.chargeback-processed",
            groupId = "reporting-service",
            containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleChargebackProcessed(
            @Payload ChargebackProcessedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.info("Processing ChargebackProcessed: transactionId={} loanId={}",
                    event.getTransactionId(), event.getLoanId());

            projectionRepository.findById(event.getTransactionId()).ifPresentOrElse(
                    projection -> {
                        projection.setStatus("CHARGEBACK");
                        projectionRepository.save(projection);
                    },
                    () -> log.warn("Projection not found for chargeback transactionId={}", event.getTransactionId())
            );
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing ChargebackProcessed event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
