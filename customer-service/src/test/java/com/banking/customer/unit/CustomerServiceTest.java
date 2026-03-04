package com.banking.customer.unit;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.customer.api.dto.CustomerRequest;
import com.banking.customer.api.dto.CustomerResponse;
import com.banking.customer.application.CustomerService;
import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import com.banking.customer.infrastructure.kafka.CustomerEventPublisher;
import com.banking.customer.infrastructure.postgres.entity.Customer;
import com.banking.customer.infrastructure.postgres.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void create_success() {
        CustomerRequest req = new CustomerRequest();
        req.setClientId("cli-001");
        req.setName("Alice");
        req.setEmail("alice@bank.com");

        Customer saved = Customer.builder()
                .id("uuid-1")
                .clientId("cli-001")
                .name("Alice")
                .email("alice@bank.com")
                .kycStatus(KycStatus.PENDING)
                .riskTier(RiskTier.LOW)
                .build();

        when(customerRepository.existsByEmail("alice@bank.com")).thenReturn(false);
        when(customerRepository.save(any())).thenReturn(saved);

        CustomerResponse result = customerService.create(req);

        assertThat(result.getClientId()).isEqualTo("cli-001");
        assertThat(result.getKycStatus()).isEqualTo(KycStatus.PENDING);
    }

    @Test
    void create_duplicateEmail_throwsValidationException() {
        CustomerRequest req = new CustomerRequest();
        req.setClientId("cli-001");
        req.setName("Alice");
        req.setEmail("alice@bank.com");

        when(customerRepository.existsByEmail("alice@bank.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(req))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(customerRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
