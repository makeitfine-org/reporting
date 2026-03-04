package com.banking.customer.application;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.customer.api.dto.CustomerRequest;
import com.banking.customer.api.dto.CustomerResponse;
import com.banking.customer.api.dto.ContactResponse;
import com.banking.customer.api.dto.KycStatusResponse;
import com.banking.customer.api.dto.KycUpdateRequest;
import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import com.banking.customer.infrastructure.kafka.CustomerEventPublisher;
import com.banking.customer.infrastructure.postgres.entity.Customer;
import com.banking.customer.infrastructure.postgres.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(String id) {
        return toResponse(getCustomerById(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Customer with email already exists: " + request.getEmail());
        }
        Customer customer = Customer.builder()
                .clientId(request.getClientId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .region(request.getRegion())
                .kycStatus(KycStatus.PENDING)
                .riskTier(RiskTier.LOW)
                .build();
        customer = customerRepository.save(customer);
        log.info("Created customer id={} clientId={}", customer.getId(), customer.getClientId());
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateProfile(String id, CustomerRequest request) {
        Customer customer = getCustomerById(id);
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setRegion(request.getRegion());
        customer = customerRepository.save(customer);
        eventPublisher.publishCustomerUpdated(customer, "PROFILE_UPDATED");
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateKyc(String id, KycUpdateRequest request) {
        Customer customer = getCustomerById(id);
        customer.setKycStatus(request.getKycStatus());
        customer.setRiskTier(request.getRiskTier());
        customer = customerRepository.save(customer);
        eventPublisher.publishCustomerUpdated(customer, "KYC_UPDATED");
        log.info("KYC updated for customerId={} status={}", id, request.getKycStatus());
        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(String id) {
        Customer customer = getCustomerById(id);
        return KycStatusResponse.builder()
                .customerId(customer.getId())
                .clientId(customer.getClientId())
                .kycStatus(customer.getKycStatus())
                .riskTier(customer.getRiskTier())
                .build();
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(String id) {
        Customer customer = getCustomerById(id);
        return ContactResponse.builder()
                .customerId(customer.getId())
                .clientId(customer.getClientId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .build();
    }

    private Customer getCustomerById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .clientId(c.getClientId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .kycStatus(c.getKycStatus())
                .riskTier(c.getRiskTier())
                .region(c.getRegion())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
