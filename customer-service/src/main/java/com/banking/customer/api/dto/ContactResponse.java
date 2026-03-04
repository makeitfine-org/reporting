package com.banking.customer.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactResponse {
    private String customerId;
    private String clientId;
    private String name;
    private String email;
    private String phone;
}
