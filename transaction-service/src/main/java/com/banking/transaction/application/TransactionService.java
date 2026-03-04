package com.banking.transaction.application;

import com.banking.commons.exception.ResourceNotFoundException;
import com.banking.transaction.api.dto.PaymentRequest;
import com.banking.transaction.api.dto.ProductRateResponse;
import com.banking.transaction.api.dto.TransactionResponse;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.infrastructure.feign.ProductServiceClient;
import com.banking.transaction.infrastructure.kafka.TransactionEventPublisher;
import com.banking.transaction.infrastructure.postgres.entity.Transaction;
import com.banking.transaction.infrastructure.postgres.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KycCheckService kycCheckService;
    private final ProductServiceClient productServiceClient;
    private final TransactionEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse createPayment(PaymentRequest request) {
        kycCheckService.assertKycApproved(request.getClientId());

        ProductRateResponse product = productServiceClient.getRate(request.getProductId());
        log.info("Product rate for productId={} rate={}", request.getProductId(), product.getInterestRate());

        Transaction tx = Transaction.builder()
                .clientId(request.getClientId())
                .productId(request.getProductId())
                .productType(product.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.COMPLETED)
                .build();
        tx = transactionRepository.save(tx);
        eventPublisher.publishTransactionCreated(tx);
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(String id) {
        return toResponse(getTransaction(id));
    }

    @Transactional
    public TransactionResponse reverse(String id) {
        Transaction tx = getTransaction(id);
        tx.setStatus(TransactionStatus.REVERSED);
        tx = transactionRepository.save(tx);
        eventPublisher.publishTransactionReversed(tx);
        return toResponse(tx);
    }

    private Transaction getTransaction(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .clientId(tx.getClientId())
                .productId(tx.getProductId())
                .productType(tx.getProductType())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .transactedAt(tx.getTransactedAt())
                .build();
    }
}
