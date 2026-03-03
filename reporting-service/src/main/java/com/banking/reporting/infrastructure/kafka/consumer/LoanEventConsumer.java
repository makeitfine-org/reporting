package com.banking.reporting.infrastructure.kafka.consumer;

import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.event.LoanDisbursedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class LoanEventConsumer {

    private final TransactionProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "reporting.loan-disbursed",
        groupId = "reporting-service",
        containerFactory = "reportingKafkaListenerFactory"
    )
    public void handleLoanDisbursed(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            LoanDisbursedEvent event = objectMapper.convertValue(payload, LoanDisbursedEvent.class);
            log.info("Processing LoanDisbursed: transactionId={} loanId={} status={}",
                    event.getTransactionId(), event.getLoanId(), event.getStatus());

            projectionRepository.findById(event.getTransactionId()).ifPresentOrElse(
                    projection -> {
                        projection.setPaymentStatus(event.getStatus());
                        projection.setLoanId(event.getLoanId());
                        projectionRepository.save(projection);
                        log.debug("Enriched projection {} with loan data loanId={}",
                                event.getTransactionId(), event.getLoanId());
                    },
                    () -> log.warn("Projection not found for loan-disbursed transactionId={}; will be created when TransactionCreated arrives",
                            event.getTransactionId())
            );
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing LoanDisbursed event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
