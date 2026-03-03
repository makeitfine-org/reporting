package com.banking.reporting.unit;

import com.banking.reporting.domain.model.ClientMetrics;
import com.banking.reporting.domain.model.ReportSnapshot;
import com.banking.reporting.infrastructure.kafka.event.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventModelTest {

    @Test
    void testKafkaEvents() {
        TransactionCreatedEvent tce = TransactionCreatedEvent.builder()
                .transactionId("tx-123")
                .clientId("cli-123")
                .amount(BigDecimal.TEN)
                .currency("USD")
                .occurredAt(Instant.now())
                .build();
        assertThat(tce.getTransactionId()).isEqualTo("tx-123");
        assertThat(tce.toString()).contains("tx-123");

        TransactionReversedEvent tre = TransactionReversedEvent.builder()
                .transactionId("tx-123")
                .reason("error")
                .occurredAt(Instant.now())
                .build();
        assertThat(tre.getTransactionId()).isEqualTo("tx-123");

        LoanDisbursedEvent lde = LoanDisbursedEvent.builder()
                .transactionId("tx-123")
                .loanId("loan-123")
                .amount(BigDecimal.TEN)
                .build();
        assertThat(lde.getLoanId()).isEqualTo("loan-123");

        ProductRateUpdatedEvent pre = ProductRateUpdatedEvent.builder()
                .productId("prod-123")
                .newRate(BigDecimal.ONE)
                .build();
        assertThat(pre.getProductId()).isEqualTo("prod-123");

        ChargebackProcessedEvent cpe = ChargebackProcessedEvent.builder()
                .transactionId("tx-123")
                .clientId("cli-123")
                .occurredAt(Instant.now())
                .build();
        assertThat(cpe.getTransactionId()).isEqualTo("tx-123");

        DeadLetterEnvelope dle = new DeadLetterEnvelope();
        dle.setOriginalTopic("topic");
        dle.setOriginalPayload("payload");
        dle.setErrorMessage("error");
        assertThat(dle.getOriginalTopic()).isEqualTo("topic");
    }

    @Test
    void testDomainModels() {
        ClientMetrics metrics = new ClientMetrics();
        metrics.setClientId("cli-123");
        metrics.setTotalTransactions(100);
        assertThat(metrics.getClientId()).isEqualTo("cli-123");
        assertThat(metrics.getTotalTransactions()).isEqualTo(100);

        ReportSnapshot snapshot = ReportSnapshot.builder()
                .id("snap-123")
                .clientId("cli-123")
                .build();
        assertThat(snapshot.getId()).isEqualTo("snap-123");
        assertThat(snapshot.getClientId()).isEqualTo("cli-123");
    }
}
