package com.banking.product.api;

import com.banking.product.api.dto.ProductRateResponse;
import com.banking.product.application.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{id}/rate")
    public ProductRateResponse getRate(@PathVariable String id) {
        return productService.getRate(id);
    }
}
