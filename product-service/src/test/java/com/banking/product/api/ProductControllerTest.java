package com.banking.product.api;

import com.banking.product.api.dto.ProductRequest;
import com.banking.product.api.dto.ProductResponse;
import com.banking.product.api.dto.RateUpdateRequest;
import com.banking.product.application.ProductService;
import com.banking.product.domain.ProductType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void list_success() throws Exception {
        ProductResponse res = ProductResponse.builder().id("1").productId("p1").build();
        when(productService.findAll(any())).thenReturn(new PageImpl<>(List.of(res), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value("p1"));
    }

    @Test
    void getById_success() throws Exception {
        ProductResponse res = ProductResponse.builder().id("1").productId("p1").build();
        when(productService.findById("1")).thenReturn(res);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("p1"));
    }

    @Test
    void create_success() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setProductId("p1");
        req.setName("Product 1");
        req.setType(ProductType.SAVINGS);
        req.setInterestRate(new BigDecimal("0.05"));

        ProductResponse res = ProductResponse.builder().id("1").productId("p1").build();
        when(productService.create(any())).thenReturn(res);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("p1"));
    }

    @Test
    void updateRate_success() throws Exception {
        RateUpdateRequest req = new RateUpdateRequest();
        req.setInterestRate(new BigDecimal("0.06"));

        ProductResponse res = ProductResponse.builder().id("1").productId("p1").interestRate(new BigDecimal("0.06")).build();
        when(productService.updateRate(eq("1"), any())).thenReturn(res);

        mockMvc.perform(put("/api/products/1/rate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestRate").value(0.06));
    }
}
