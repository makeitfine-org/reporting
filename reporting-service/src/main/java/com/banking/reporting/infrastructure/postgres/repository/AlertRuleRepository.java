package com.banking.reporting.infrastructure.postgres.repository;

import com.banking.reporting.infrastructure.postgres.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, String> {

    List<AlertRule> findByClientIdAndEnabled(String clientId, boolean enabled);

    long countByClientIdAndEnabled(String clientId, boolean enabled);
}
