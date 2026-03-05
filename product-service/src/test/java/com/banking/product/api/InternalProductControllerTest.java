package com.banking.product.api;

import com.banking.product.api.dto.ProductRateResponse;
import com.banking.product.application.ProductService;
import com.banking.product.domain.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private InternalProductController internalProductController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalProductController).build();
    }

    @Test
    void getRate_success() throws Exception {
        ProductRateResponse res = ProductRateResponse.builder()
                .productId("p1")
                .type(ProductType.MORTGAGE)
                .interestRate(new BigDecimal("0.04"))
                .active(true)
                .build();
        when(productService.getRate("1")).thenReturn(res);

        mockMvc.perform(get("/internal/products/1/rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("p1"))
                .andExpect(jsonPath("$.interestRate").value(0.04));
    }
}
