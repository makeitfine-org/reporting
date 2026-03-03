package com.banking.reporting.application;

import com.banking.reporting.api.dto.DashboardDto;
import com.banking.reporting.api.dto.TransactionSummaryDto;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.postgres.repository.AlertRuleRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionProjectionRepository projectionRepository;
    private final AlertRuleRepository alertRuleRepository;

    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "dashboardFallback")
    public DashboardDto getDashboard(String clientId) {
        log.info("Dashboard requested for clientId={}", clientId);

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant now = Instant.now();

        List<TransactionProjection> todayTransactions =
                projectionRepository.findByClientIdAndTransactedAtBetween(clientId, startOfDay, now);

        BigDecimal totalVolumeToday = todayTransactions.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeAlerts = alertRuleRepository.countByClientIdAndEnabled(clientId, true);

        List<TransactionSummaryDto> recentTransactions = todayTransactions.stream()
                .sorted((a, b) -> b.getTransactedAt().compareTo(a.getTransactedAt()))
                .limit(10)
                .map(p -> TransactionSummaryDto.builder()
                        .transactionId(p.getTransactionId())
                        .clientId(p.getClientId())
                        .status(p.getStatus())
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .transactedAt(p.getTransactedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.builder()
                .clientId(clientId)
                .totalVolumeToday(totalVolumeToday)
                .transactionCountToday(todayTransactions.size())
                .activeAlerts(activeAlerts)
                .recentTransactions(recentTransactions)
                .generatedAt(Instant.now())
                .build();
    }

    public DashboardDto dashboardFallback(String clientId, Throwable t) {
        log.error("Circuit breaker open for dashboard: clientId={} error={}", clientId, t.getMessage());
        return DashboardDto.builder()
                .clientId(clientId)
                .totalVolumeToday(BigDecimal.ZERO)
                .transactionCountToday(0)
                .activeAlerts(0)
                .recentTransactions(List.of())
                .generatedAt(Instant.now())
                .build();
    }
}
