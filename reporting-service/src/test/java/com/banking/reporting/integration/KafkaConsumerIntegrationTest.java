package com.banking.reporting.integration;

import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.event.TransactionCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class KafkaConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TransactionProjectionRepository projectionRepository;

    @Test
    void publishTransactionCreatedEvent_consumerCreatesESProjection() {
        String transactionId = "tx-integration-001";
        String clientId = "cli-integration-001";

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .eventType("TransactionCreated")
                .schemaVersion("1.0")
                .transactionId(transactionId)
                .clientId(clientId)
                .amount(BigDecimal.valueOf(1500))
                .currency("USD")
                .productId("prod-001")
                .productType("LOAN")
                .occurredAt(Instant.now())
                .build();

        kafkaTemplate.send("reporting.transaction-created", clientId, event);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var projection = projectionRepository.findById(transactionId);
            assertThat(projection).isPresent();
            assertThat(projection.get().getClientId()).isEqualTo(clientId);
            assertThat(projection.get().getStatus()).isEqualTo("COMPLETED");
            assertThat(projection.get().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        });
    }
}
