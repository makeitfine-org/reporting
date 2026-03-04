package com.banking.customer.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank
    private String clientId;
    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String email;
    private String phone;
    private String region;
}
