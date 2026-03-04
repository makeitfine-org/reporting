package com.banking.product.infrastructure.postgres.repository;

import com.banking.product.infrastructure.postgres.entity.BankProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<BankProduct, String> {
    Optional<BankProduct> findByProductId(String productId);

    boolean existsByProductId(String productId);
}
