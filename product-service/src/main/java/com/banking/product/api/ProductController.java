package com.banking.product.api;

import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.api.dto.RateUpdateRequest;
import com.banking.product.application.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> list(Pageable pageable) {
        return productService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable String id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}/rate")
    public ProductResponse updateRate(@PathVariable String id,
                                      @Valid @RequestBody RateUpdateRequest request) {
        return productService.updateRate(id, request);
    }
}
