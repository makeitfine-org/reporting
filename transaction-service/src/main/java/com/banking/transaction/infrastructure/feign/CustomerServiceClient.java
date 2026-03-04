package com.banking.transaction.infrastructure.feign;

import com.banking.commons.feign.InternalFeignConfig;
import com.banking.transaction.api.dto.KycStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-service",
        url = "${services.customer.url:http://localhost:8081}",
        configuration = InternalFeignConfig.class
)
public interface CustomerServiceClient {

    @GetMapping("/internal/customers/{id}/kyc-status")
    KycStatusResponse getKycStatus(@PathVariable("id") String customerId);
}
