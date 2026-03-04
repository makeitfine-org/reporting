package com.banking.notification.integration;

import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.infrastructure.kafka.event.TransactionCreatedEvent;
import com.banking.notification.infrastructure.postgres.repository.NotificationJobRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1,
        topics = {"notification.transaction-created", "notification.customer-updated"})
class KafkaNotificationConsumerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("notifications")
            .withUsername("notifications")
            .withPassword("notifications");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private NotificationJobRepository jobRepository;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        wireMock.stubFor(get(urlPathMatching("/internal/customers/.*/contact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"customerId":"cli-001","clientId":"cli-001","name":"Alice",
                                 "email":"alice@bank.com","phone":"+49123456789"}
                                """)));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("services.customer.url", () -> "http://localhost:" + wireMock.port());
    }

    @Test
    void transactionCreatedEvent_createsNotificationJob() {
        TransactionCreatedEvent event = new TransactionCreatedEvent();
        event.setTransactionId("tx-it-001");
        event.setClientId("cli-001");
        event.setAmount(new BigDecimal("5000"));
        event.setCurrency("EUR");
        event.setTransactedAt(Instant.parse("2026-01-01T10:00:00Z"));

        kafkaTemplate.send("notification.transaction-created", "cli-001", event);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> jobRepository.findByStatus(NotificationStatus.SENT).size() > 0
                        || jobRepository.findByStatus(NotificationStatus.FAILED).size() > 0);

        assertThat(jobRepository.count()).isGreaterThan(0);
    }
}
