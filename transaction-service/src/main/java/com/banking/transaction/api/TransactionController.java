package com.banking.transaction.api;

import com.banking.transaction.api.dto.PaymentRequest;
import com.banking.transaction.api.dto.TransactionResponse;
import com.banking.transaction.application.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createPayment(@Valid @RequestBody PaymentRequest request) {
        return transactionService.createPayment(request);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable String id) {
        return transactionService.findById(id);
    }

    @PostMapping("/{id}/reverse")
    public TransactionResponse reverse(@PathVariable String id) {
        return transactionService.reverse(id);
    }
}
