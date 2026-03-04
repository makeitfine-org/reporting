package com.banking.customer.api;

import com.banking.customer.api.dto.CustomerRequest;
import com.banking.customer.api.dto.CustomerResponse;
import com.banking.customer.api.dto.KycUpdateRequest;
import com.banking.customer.application.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Page<CustomerResponse> list(Pageable pageable) {
        return customerService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable String id) {
        return customerService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateProfile(@PathVariable String id,
                                          @Valid @RequestBody CustomerRequest request) {
        return customerService.updateProfile(id, request);
    }

    @PutMapping("/{id}/kyc")
    public CustomerResponse updateKyc(@PathVariable String id,
                                      @Valid @RequestBody KycUpdateRequest request) {
        return customerService.updateKyc(id, request);
    }
}
