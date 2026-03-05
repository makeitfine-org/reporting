package com.banking.transaction.unit;

import com.banking.transaction.api.TransactionController;
import com.banking.transaction.api.dto.TransactionResponse;
import com.banking.transaction.application.TransactionService;
import com.banking.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createPayment_returns201() throws Exception {
        TransactionResponse resp = TransactionResponse.builder()
                .id("tx-1").clientId("c-1").status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("5000")).currency("EUR").build();
        when(transactionService.createPayment(any())).thenReturn(resp);

        mockMvc.perform(post("/api/transactions/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"c-1","productId":"p-1",
                                 "amount":5000,"currency":"EUR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tx-1"));
    }

    @Test
    void getById_returns200() throws Exception {
        TransactionResponse resp = TransactionResponse.builder()
                .id("tx-2").clientId("c-2").status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000")).currency("USD").build();
        when(transactionService.findById("tx-2")).thenReturn(resp);

        mockMvc.perform(get("/api/transactions/tx-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tx-2"));
    }

    @Test
    void reverse_returns200() throws Exception {
        TransactionResponse resp = TransactionResponse.builder()
                .id("tx-3").clientId("c-3").status(TransactionStatus.REVERSED)
                .amount(new BigDecimal("2000")).currency("GBP").build();
        when(transactionService.reverse("tx-3")).thenReturn(resp);

        mockMvc.perform(post("/api/transactions/tx-3/reverse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }
}
