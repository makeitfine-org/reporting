package com.banking.product.unit;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.commons.exception.ValidationException;
import com.banking.product.api.dto.ProductRateResponse;
import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.api.dto.RateUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
    void findById_success() {
        BankProduct product = BankProduct.builder()
                .id("uuid-1")
                .productId("prod-01")
                .build();
        when(productRepository.findById("uuid-1")).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById("uuid-1");

        assertThat(result.getProductId()).isEqualTo("prod-01");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_success() {
        BankProduct product = BankProduct.builder()
                .id("uuid-1")
                .productId("prod-01")
                .build();
        Page<BankProduct> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductId()).isEqualTo("prod-01");
    }

    @Test
    void updateRate_success() {
        BankProduct product = BankProduct.builder()
                .id("uuid-1")
                .productId("prod-01")
                .interestRate(new BigDecimal("0.01"))
                .build();
        RateUpdateRequest req = new RateUpdateRequest();
        req.setInterestRate(new BigDecimal("0.02"));

        when(productRepository.findById("uuid-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.updateRate("uuid-1", req);

        assertThat(result.getInterestRate()).isEqualByComparingTo("0.02");
        assertThat(result.getPreviousRate()).isEqualByComparingTo("0.01");
        verify(eventPublisher).publishRateUpdated(any());
    }

    @Test
    void getRate_success() {
        BankProduct product = BankProduct.builder()
                .id("uuid-1")
                .productId("prod-01")
                .interestRate(new BigDecimal("0.05"))
                .type(ProductType.SAVINGS)
                .active(true)
                .build();
        when(productRepository.findById("uuid-1")).thenReturn(Optional.of(product));

        ProductRateResponse result = productService.getRate("uuid-1");

        assertThat(result.getProductId()).isEqualTo("prod-01");
        assertThat(result.getInterestRate()).isEqualByComparingTo("0.05");
    }
}
