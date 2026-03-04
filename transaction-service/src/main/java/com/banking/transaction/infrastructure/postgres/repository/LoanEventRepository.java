package com.banking.transaction.infrastructure.postgres.repository;

import com.banking.transaction.infrastructure.postgres.entity.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanEventRepository extends JpaRepository<LoanEvent, String> {
}
