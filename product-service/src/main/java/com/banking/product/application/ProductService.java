package com.banking.product.application;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.product.api.dto.ProductRateResponse;
import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.api.dto.RateUpdateRequest;
import com.banking.product.infrastructure.kafka.ProductEventPublisher;
import com.banking.product.infrastructure.postgres.entity.BankProduct;
import com.banking.product.infrastructure.postgres.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(String id) {
        return toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByProductId(request.getProductId())) {
            throw new ValidationException("Product already exists: " + request.getProductId());
        }
        BankProduct product = BankProduct.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .type(request.getType())
                .interestRate(request.getInterestRate())
                .effectiveDate(Instant.now())
                .active(true)
                .build();
        product = productRepository.save(product);
        log.info("Created product id={} productId={}", product.getId(), product.getProductId());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateRate(String id, RateUpdateRequest request) {
        BankProduct product = getProduct(id);
        product.setPreviousRate(product.getInterestRate());
        product.setInterestRate(request.getInterestRate());
        product.setEffectiveDate(Instant.now());
        product = productRepository.save(product);
        eventPublisher.publishRateUpdated(product);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductRateResponse getRate(String id) {
        BankProduct product = getProduct(id);
        return ProductRateResponse.builder()
                .productId(product.getProductId())
                .type(product.getType())
                .interestRate(product.getInterestRate())
                .active(product.isActive())
                .build();
    }

    private BankProduct getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private ProductResponse toResponse(BankProduct p) {
        return ProductResponse.builder()
                .id(p.getId())
                .productId(p.getProductId())
                .name(p.getName())
                .type(p.getType())
                .interestRate(p.getInterestRate())
                .previousRate(p.getPreviousRate())
                .active(p.isActive())
                .effectiveDate(p.getEffectiveDate())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
