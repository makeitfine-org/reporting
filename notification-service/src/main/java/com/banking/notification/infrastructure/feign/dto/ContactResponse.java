package com.banking.notification.infrastructure.feign.dto;

import lombok.Data;

@Data
public class ContactResponse {
    private String customerId;
    private String clientId;
    private String name;
    private String email;
    private String phone;
}
