package com.banking.reporting.infrastructure.elasticsearch.repository;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;

import java.time.Instant;

public interface TransactionProjectionRepositoryCustom {

    FinancialReportDto aggregateFinancialReport(String clientId, Instant from, Instant to);

    RevenueReportDto aggregateRevenueReport(String clientId, Instant from, Instant to);
}
