package com.banking.reporting.infrastructure.postgres.repository;

import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportConfigRepository extends JpaRepository<ReportConfig, String> {

    Optional<ReportConfig> findByClientId(String clientId);

    boolean existsByClientId(String clientId);
}
