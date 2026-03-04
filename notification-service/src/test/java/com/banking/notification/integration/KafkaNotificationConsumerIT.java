package com.banking.notification.integration;

import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.infrastructure.kafka.event.TransactionCreatedEvent;
import com.banking.notification.infrastructure.postgres.repository.NotificationJobRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
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

    // Both started in static initializer so ports are known before @DynamicPropertySource runs
    static final GreenMail greenMail;
    static final WireMockServer wireMock;

    static {
        greenMail = new GreenMail(new ServerSetup(0, "localhost", ServerSetup.PROTOCOL_SMTP));
        greenMail.start();

        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private NotificationJobRepository jobRepository;

    @BeforeAll
    static void configureStubs() {
        wireMock.stubFor(get(urlPathMatching("/internal/customers/.*/contact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"customerId":"cli-001","clientId":"cli-001","name":"Alice",
                                 "email":"alice@bank.com","phone":"+49123456789"}
                                """)));
        wireMock.stubFor(post(urlEqualTo("/send"))
                .willReturn(aResponse().withStatus(200)));
    }

    @AfterAll
    static void stopServers() {
        wireMock.stop();
        greenMail.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("services.customer.url", () -> "http://localhost:" + wireMock.port());
        registry.add("sms.gateway.url", () -> "http://localhost:" + wireMock.port() + "/send");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
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
                .until(() -> !jobRepository.findByStatus(NotificationStatus.SENT).isEmpty());

        assertThat(jobRepository.findByStatus(NotificationStatus.SENT)).hasSize(1);
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }
}
