package com.banking.product.unit;

import com.banking.product.api.dto.ProductRateResponse;
import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.api.dto.RateUpdateRequest;
import com.banking.product.domain.ProductType;
import com.banking.product.infrastructure.kafka.event.ProductRateUpdatedEvent;
import com.banking.product.infrastructure.postgres.entity.BankProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {

    @Test
    void testBankProduct() {
        BankProduct product = BankProduct.builder()
                .id("1")
                .productId("p1")
                .name("n1")
                .type(ProductType.SAVINGS)
                .interestRate(BigDecimal.TEN)
                .previousRate(BigDecimal.ONE)
                .active(true)
                .effectiveDate(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThat(product.getId()).isEqualTo("1");
        assertThat(product.toString()).contains("productId=p1");
        assertThat(product.hashCode()).isNotZero();
        assertThat(product.equals(product)).isTrue();
        assertThat(product.equals(new Object())).isFalse();

        BankProduct other = new BankProduct();
        other.setId("1");
        assertThat(product.equals(other)).isFalse(); // due to other fields
    }

    @Test
    void testProductRequest() {
        ProductRequest req = new ProductRequest();
        req.setProductId("p1");
        req.setName("n1");
        req.setType(ProductType.MORTGAGE);
        req.setInterestRate(BigDecimal.ZERO);

        assertThat(req.getProductId()).isEqualTo("p1");
    }

    @Test
    void testProductResponse() {
        ProductResponse res = ProductResponse.builder()
                .id("1")
                .productId("p1")
                .build();
        assertThat(res.getProductId()).isEqualTo("p1");
    }

    @Test
    void testProductRateResponse() {
        ProductRateResponse res = ProductRateResponse.builder()
                .productId("p1")
                .build();
        assertThat(res.getProductId()).isEqualTo("p1");
    }

    @Test
    void testRateUpdateRequest() {
        RateUpdateRequest req = new RateUpdateRequest();
        req.setInterestRate(BigDecimal.ONE);
        assertThat(req.getInterestRate()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testProductRateUpdatedEvent() {
        ProductRateUpdatedEvent event = ProductRateUpdatedEvent.builder()
                .productId("p1")
                .build();
        assertThat(event.getProductId()).isEqualTo("p1");
        assertThat(event.toString()).contains("p1");
    }

    @Test
    void testProductType() {
        assertThat(ProductType.valueOf("MORTGAGE")).isEqualTo(ProductType.MORTGAGE);
        assertThat(ProductType.values()).hasSize(4);
    }
}
