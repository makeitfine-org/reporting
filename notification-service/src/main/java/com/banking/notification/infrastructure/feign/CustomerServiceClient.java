package com.banking.notification.infrastructure.feign;

import com.banking.commons.feign.InternalFeignConfig;
import com.banking.notification.infrastructure.feign.dto.ContactResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-service",
        url = "${services.customer.url:http://localhost:8081}",
        configuration = InternalFeignConfig.class
)
public interface CustomerServiceClient {

    @GetMapping("/internal/customers/{id}/contact")
    ContactResponse getContact(@PathVariable("id") String customerId);
}
