package com.banking.transaction.infrastructure.postgres.repository;

import com.banking.transaction.infrastructure.postgres.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
