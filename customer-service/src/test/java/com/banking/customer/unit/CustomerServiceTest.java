package com.banking.customer.unit;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.customer.api.dto.CustomerRequest;
import com.banking.customer.api.dto.CustomerResponse;
import com.banking.customer.api.dto.KycUpdateRequest;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @Test
    void findAll_returnsMappedPage() {
        Customer customer = Customer.builder()
                .id("id-1").clientId("cli-1").name("Bob")
                .email("bob@bank.com").kycStatus(KycStatus.APPROVED).riskTier(RiskTier.LOW)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer)));

        Page<CustomerResponse> result = customerService.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getClientId()).isEqualTo("cli-1");
    }

    @Test
    void findById_success() {
        Customer customer = Customer.builder()
                .id("id-2").clientId("cli-2").name("Carol")
                .email("carol@bank.com").kycStatus(KycStatus.PENDING).riskTier(RiskTier.LOW)
                .build();
        when(customerRepository.findById("id-2")).thenReturn(Optional.of(customer));

        CustomerResponse result = customerService.findById("id-2");

        assertThat(result.getId()).isEqualTo("id-2");
        assertThat(result.getKycStatus()).isEqualTo(KycStatus.PENDING);
    }

    @Test
    void updateProfile_success() {
        Customer existing = Customer.builder()
                .id("id-3").clientId("cli-3").name("Dan")
                .email("dan@bank.com").kycStatus(KycStatus.PENDING).riskTier(RiskTier.LOW)
                .build();
        CustomerRequest req = new CustomerRequest();
        req.setClientId("cli-3");
        req.setName("Daniel");
        req.setEmail("dan@bank.com");
        req.setPhone("555-1234");

        when(customerRepository.findById("id-3")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any())).thenReturn(existing);

        CustomerResponse result = customerService.updateProfile("id-3", req);

        assertThat(result).isNotNull();
        verify(eventPublisher).publishCustomerUpdated(any(), eq("PROFILE_UPDATED"));
    }

    @Test
    void updateProfile_notFound_throws() {
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());

        CustomerRequest req = new CustomerRequest();
        req.setName("X");
        req.setEmail("x@bank.com");

        assertThatThrownBy(() -> customerService.updateProfile("missing", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateKyc_success() {
        Customer customer = Customer.builder()
                .id("id-4").clientId("cli-4").name("Eve")
                .email("eve@bank.com").kycStatus(KycStatus.PENDING).riskTier(RiskTier.LOW)
                .build();
        KycUpdateRequest kycReq = new KycUpdateRequest();
        kycReq.setKycStatus(KycStatus.APPROVED);
        kycReq.setRiskTier(RiskTier.MEDIUM);

        when(customerRepository.findById("id-4")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any())).thenReturn(customer);

        CustomerResponse result = customerService.updateKyc("id-4", kycReq);

        assertThat(result).isNotNull();
        verify(eventPublisher).publishCustomerUpdated(any(), eq("KYC_UPDATED"));
    }

    @Test
    void getKycStatus_success() {
        Customer customer = Customer.builder()
                .id("id-5").clientId("cli-5").name("Frank")
                .email("frank@bank.com").kycStatus(KycStatus.APPROVED).riskTier(RiskTier.HIGH)
                .build();
        when(customerRepository.findById("id-5")).thenReturn(Optional.of(customer));

        var result = customerService.getKycStatus("id-5");

        assertThat(result.getCustomerId()).isEqualTo("id-5");
        assertThat(result.getKycStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(result.getRiskTier()).isEqualTo(RiskTier.HIGH);
    }

    @Test
    void getContact_success() {
        Customer customer = Customer.builder()
                .id("id-6").clientId("cli-6").name("Grace")
                .email("grace@bank.com").phone("555-9999").kycStatus(KycStatus.PENDING).riskTier(RiskTier.LOW)
                .build();
        when(customerRepository.findById("id-6")).thenReturn(Optional.of(customer));

        var result = customerService.getContact("id-6");

        assertThat(result.getCustomerId()).isEqualTo("id-6");
        assertThat(result.getName()).isEqualTo("Grace");
        assertThat(result.getEmail()).isEqualTo("grace@bank.com");
        assertThat(result.getPhone()).isEqualTo("555-9999");
    }
}
