package com.banking.customer.unit;

import com.banking.customer.api.CustomerController;
import com.banking.customer.api.dto.CustomerRequest;
import com.banking.customer.api.dto.CustomerResponse;
import com.banking.customer.api.dto.KycUpdateRequest;
import com.banking.customer.application.CustomerService;
import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    @Test
    void list_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 10);
        CustomerResponse response = CustomerResponse.builder().id("id-1").clientId("cli-1").build();
        Page<CustomerResponse> page = new PageImpl<>(List.of(response));
        when(customerService.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerController.list(pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getById_delegatesToService() {
        CustomerResponse response = CustomerResponse.builder().id("id-1").clientId("cli-1").build();
        when(customerService.findById("id-1")).thenReturn(response);

        CustomerResponse result = customerController.getById("id-1");

        assertThat(result).isSameAs(response);
    }

    @Test
    void create_delegatesToService() {
        CustomerRequest request = new CustomerRequest();
        request.setClientId("cli-1");
        request.setName("Alice");
        request.setEmail("alice@bank.com");
        CustomerResponse response = CustomerResponse.builder().id("id-1").clientId("cli-1").build();
        when(customerService.create(request)).thenReturn(response);

        CustomerResponse result = customerController.create(request);

        assertThat(result).isSameAs(response);
    }

    @Test
    void updateProfile_delegatesToService() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Alice Updated");
        request.setEmail("alice@bank.com");
        CustomerResponse response = CustomerResponse.builder().id("id-1").clientId("cli-1").build();
        when(customerService.updateProfile("id-1", request)).thenReturn(response);

        CustomerResponse result = customerController.updateProfile("id-1", request);

        assertThat(result).isSameAs(response);
    }

    @Test
    void updateKyc_delegatesToService() {
        KycUpdateRequest request = new KycUpdateRequest();
        request.setKycStatus(KycStatus.APPROVED);
        request.setRiskTier(RiskTier.MEDIUM);
        CustomerResponse response = CustomerResponse.builder()
                .id("id-1").clientId("cli-1").kycStatus(KycStatus.APPROVED).build();
        when(customerService.updateKyc("id-1", request)).thenReturn(response);

        CustomerResponse result = customerController.updateKyc("id-1", request);

        assertThat(result).isSameAs(response);
    }
}
