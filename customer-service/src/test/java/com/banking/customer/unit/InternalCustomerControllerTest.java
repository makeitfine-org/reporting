package com.banking.customer.unit;

import com.banking.customer.api.InternalCustomerController;
import com.banking.customer.api.dto.ContactResponse;
import com.banking.customer.api.dto.KycStatusResponse;
import com.banking.customer.application.CustomerService;
import com.banking.customer.domain.KycStatus;
import com.banking.customer.domain.RiskTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private InternalCustomerController internalCustomerController;

    @Test
    void getKycStatus_delegatesToService() {
        KycStatusResponse response = KycStatusResponse.builder()
                .customerId("id-1").clientId("cli-1")
                .kycStatus(KycStatus.APPROVED).riskTier(RiskTier.LOW)
                .build();
        when(customerService.getKycStatus("id-1")).thenReturn(response);

        KycStatusResponse result = internalCustomerController.getKycStatus("id-1");

        assertThat(result).isSameAs(response);
    }

    @Test
    void getContact_delegatesToService() {
        ContactResponse response = ContactResponse.builder()
                .customerId("id-1").clientId("cli-1")
                .name("Alice").email("alice@bank.com")
                .build();
        when(customerService.getContact("id-1")).thenReturn(response);

        ContactResponse result = internalCustomerController.getContact("id-1");

        assertThat(result).isSameAs(response);
    }
}
