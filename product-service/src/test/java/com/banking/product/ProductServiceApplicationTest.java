package com.banking.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceApplicationTest {

    @Test
    void contextLoads() {
        ProductServiceApplication application = new ProductServiceApplication();
        assertThat(application).isNotNull();
    }
}
