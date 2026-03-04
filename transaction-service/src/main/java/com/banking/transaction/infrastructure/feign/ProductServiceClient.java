package com.banking.transaction.infrastructure.feign;

import com.banking.commons.feign.InternalFeignConfig;
import com.banking.transaction.api.dto.ProductRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        url = "${services.product.url:http://localhost:8083}",
        configuration = InternalFeignConfig.class
)
public interface ProductServiceClient {

    @GetMapping("/internal/products/{id}/rate")
    ProductRateResponse getRate(@PathVariable("id") String productId);
}
