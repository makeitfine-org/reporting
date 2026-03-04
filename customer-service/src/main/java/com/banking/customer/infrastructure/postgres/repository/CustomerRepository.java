package com.banking.customer.infrastructure.postgres.repository;

import com.banking.customer.infrastructure.postgres.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByClientId(String clientId);

    boolean existsByEmail(String email);
}
