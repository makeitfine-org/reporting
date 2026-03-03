package com.banking.reporting.unit;

import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.consumer.LoanEventConsumer;
import com.banking.reporting.infrastructure.kafka.event.LoanDisbursedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanEventConsumerTest {

    @Mock
    private TransactionProjectionRepository projectionRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private LoanEventConsumer loanEventConsumer;

    @Test
    void handleLoanDisbursed_updatesProjection_whenExists() {
        LoanDisbursedEvent event = LoanDisbursedEvent.builder()
                .transactionId("tx-001")
                .loanId("loan-001")
                .status("DISBURSED")
                .build();

        TransactionProjection projection = new TransactionProjection();
        projection.setTransactionId("tx-001");

        when(projectionRepository.findById("tx-001")).thenReturn(Optional.of(projection));

        loanEventConsumer.handleLoanDisbursed(event, "topic", acknowledgment);

        verify(projectionRepository).save(any(TransactionProjection.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleLoanDisbursed_acknowledges_whenProjectionNotFound() {
        LoanDisbursedEvent event = LoanDisbursedEvent.builder()
                .transactionId("tx-001")
                .loanId("loan-001")
                .status("DISBURSED")
                .build();

        when(projectionRepository.findById("tx-001")).thenReturn(Optional.empty());

        loanEventConsumer.handleLoanDisbursed(event, "topic", acknowledgment);

        verify(projectionRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
