package com.banking.transaction.unit;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.transaction.api.dto.PaymentRequest;
import com.banking.transaction.api.dto.ProductRateResponse;
import com.banking.transaction.api.dto.TransactionResponse;
import com.banking.transaction.application.KycCheckService;
import com.banking.transaction.application.TransactionService;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.infrastructure.feign.ProductServiceClient;
import com.banking.transaction.infrastructure.kafka.TransactionEventPublisher;
import com.banking.transaction.infrastructure.postgres.entity.Transaction;
import com.banking.transaction.infrastructure.postgres.repository.TransactionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private KycCheckService kycCheckService;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private TransactionEventPublisher eventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createPayment_success() {
        PaymentRequest req = new PaymentRequest();
        req.setClientId("cli-001");
        req.setProductId("prod-001");
        req.setAmount(new BigDecimal("5000"));
        req.setCurrency("EUR");

        ProductRateResponse rate = new ProductRateResponse();
        rate.setProductId("prod-001");
        rate.setType("MORTGAGE");
        rate.setInterestRate(new BigDecimal("0.0375"));
        rate.setActive(true);

        Transaction saved = Transaction.builder().id("tx-uuid-1").clientId("cli-001").productId("prod-001").productType("MORTGAGE").amount(new BigDecimal("5000")).currency("EUR").status(TransactionStatus.COMPLETED).build();

        doNothing().when(kycCheckService).assertKycApproved("cli-001");
        when(productServiceClient.getRate("prod-001")).thenReturn(rate);
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse result = transactionService.createPayment(req);

        assertThat(result.getId()).isEqualTo("tx-uuid-1");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(eventPublisher).publishTransactionCreated(any());
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById("bad-id")).isInstanceOf(ResourceNotFoundException.class);
    }
}
