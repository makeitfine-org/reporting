package com.banking.customer.api;

import com.banking.customer.api.dto.ContactResponse;
import com.banking.customer.api.dto.KycStatusResponse;
import com.banking.customer.application.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    @GetMapping("/{id}/kyc-status")
    public KycStatusResponse getKycStatus(@PathVariable String id) {
        return customerService.getKycStatus(id);
    }

    @GetMapping("/{id}/contact")
    public ContactResponse getContact(@PathVariable String id) {
        return customerService.getContact(id);
    }
}
