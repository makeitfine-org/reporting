package com.banking.product.unit;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.application.ProductService;
import com.banking.product.domain.ProductType;
import com.banking.product.infrastructure.kafka.ProductEventPublisher;
import com.banking.product.infrastructure.postgres.entity.BankProduct;
import com.banking.product.infrastructure.postgres.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_success() {
        ProductRequest req = new ProductRequest();
        req.setProductId("prod-mortgage-01");
        req.setName("Standard Mortgage");
        req.setType(ProductType.MORTGAGE);
        req.setInterestRate(new BigDecimal("0.0375"));

        BankProduct saved = BankProduct.builder()
                .id("uuid-1")
                .productId("prod-mortgage-01")
                .name("Standard Mortgage")
                .type(ProductType.MORTGAGE)
                .interestRate(new BigDecimal("0.0375"))
                .active(true)
                .build();

        when(productRepository.existsByProductId("prod-mortgage-01")).thenReturn(false);
        when(productRepository.save(any())).thenReturn(saved);

        ProductResponse result = productService.create(req);

        assertThat(result.getProductId()).isEqualTo("prod-mortgage-01");
        assertThat(result.getInterestRate()).isEqualByComparingTo("0.0375");
    }

    @Test
    void create_duplicate_throwsValidationException() {
        ProductRequest req = new ProductRequest();
        req.setProductId("prod-001");
        req.setName("Test");
        req.setType(ProductType.SAVINGS);
        req.setInterestRate(BigDecimal.ONE);

        when(productRepository.existsByProductId("prod-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(req))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
