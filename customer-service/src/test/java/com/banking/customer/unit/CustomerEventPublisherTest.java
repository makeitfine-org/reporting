package com.banking.customer.unit;

import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import com.banking.customer.infrastructure.kafka.CustomerEventPublisher;
import com.banking.customer.infrastructure.postgres.entity.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CustomerEventPublisher publisher;

    @Test
    void publishCustomerUpdated_sendsToCorrectTopic() {
        Customer customer = Customer.builder()
                .id("uuid-1")
                .clientId("cli-001")
                .email("alice@bank.com")
                .kycStatus(KycStatus.APPROVED)
                .riskTier(RiskTier.LOW)
                .build();

        publisher.publishCustomerUpdated(customer, "KYC_UPDATED");

        verify(kafkaTemplate).send(eq("notification.customer-updated"), eq("cli-001"), any());
    }
}
