package com.banking.transaction.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1,
        topics = {"reporting.transaction-created", "notification.transaction-created",
                "reporting.transaction-reversed"})
class TransactionControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("transactions")
            .withUsername("transactions")
            .withPassword("transactions");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        wireMock.stubFor(WireMock.get(urlPathMatching("/internal/customers/.*/kyc-status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"customerId":"cli-it-001","clientId":"cli-it-001","kycStatus":"APPROVED","riskTier":"LOW"}
                                """)));
        wireMock.stubFor(WireMock.get(urlPathMatching("/internal/products/.*/rate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId":"prod-001","type":"MORTGAGE","interestRate":0.0375,"active":true}
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
        registry.add("services.product.url", () -> "http://localhost:" + wireMock.port());
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void createPayment_returns201() throws Exception {
        String body = """
                {
                  "clientId": "cli-it-001",
                  "productId": "prod-001",
                  "amount": 15000,
                  "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value("cli-it-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getTransaction_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/transactions/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
