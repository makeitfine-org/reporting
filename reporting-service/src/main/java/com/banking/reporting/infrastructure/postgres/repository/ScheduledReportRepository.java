package com.banking.reporting.infrastructure.postgres.repository;

import com.banking.reporting.infrastructure.postgres.entity.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, String> {

    List<ScheduledReport> findByClientIdAndEnabled(String clientId, boolean enabled);
}
