package com.banking.reporting.application;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.api.dto.TransactionSummaryDto;
import com.banking.reporting.domain.exception.ResourceNotFoundException;
import com.banking.reporting.domain.exception.ServiceUnavailableException;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import com.banking.reporting.infrastructure.postgres.repository.ReportConfigRepository;
import com.banking.reporting.infrastructure.redis.ReportCacheService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private static final String ELASTICSEARCH_INSTANCE = "elasticsearch";

    private final TransactionProjectionRepository projectionRepository;
    private final ReportConfigRepository reportConfigRepository;
    private final ReportCacheService cacheService;

    public FinancialReportDto getFinancialReport(String clientId, String period) {
        log.info("Financial report requested: clientId={} period={}", clientId, period);

        // 1. Check Redis cache
        var cached = cacheService.getFinancialReport(clientId, period);
        if (cached.isPresent()) {
            log.debug("Cache HIT for financial report: clientId={} period={}", clientId, period);
            FinancialReportDto result = cached.get();
            result.setCacheSource("REDIS");
            return result;
        }

        // 2. Load report config from PostgreSQL
        ReportConfig config = reportConfigRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportConfig", clientId));

        // 3. Parse period and execute ES aggregation
        Instant[] range = parsePeriod(period);
        FinancialReportDto report = executeFinancialAggregation(clientId, range[0], range[1]);
        report.setGeneratedAt(Instant.now());
        report.setCacheSource("ELASTICSEARCH");

        // 4. Store in Redis (5 min TTL)
        cacheService.putFinancialReport(clientId, period, report);

        return report;
    }

    @CircuitBreaker(name = ELASTICSEARCH_INSTANCE, fallbackMethod = "financialAggregationFallback")
    @Retry(name = ELASTICSEARCH_INSTANCE)
    @Bulkhead(name = ELASTICSEARCH_INSTANCE)
    public FinancialReportDto executeFinancialAggregation(String clientId, Instant from, Instant to) {
        return projectionRepository.aggregateFinancialReport(clientId, from, to);
    }

    public FinancialReportDto financialAggregationFallback(String clientId, Instant from, Instant to, Throwable t) {
        log.error("Circuit breaker open for ES financial aggregation: clientId={} error={}", clientId, t.getMessage());
        throw new ServiceUnavailableException("Elasticsearch is unavailable. Please try again later.", t);
    }

    public RevenueReportDto getRevenueReport(String clientId, String period) {
        log.info("Revenue report requested: clientId={} period={}", clientId, period);

        var cached = cacheService.getRevenueReport(clientId, period);
        if (cached.isPresent()) {
            log.debug("Cache HIT for revenue report: clientId={} period={}", clientId, period);
            RevenueReportDto result = cached.get();
            result.setCacheSource("REDIS");
            return result;
        }

        reportConfigRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportConfig", clientId));

        Instant[] range = parsePeriod(period);
        RevenueReportDto report = executeRevenueAggregation(clientId, range[0], range[1]);
        report.setGeneratedAt(Instant.now());
        report.setCacheSource("ELASTICSEARCH");

        cacheService.putRevenueReport(clientId, period, report);
        return report;
    }

    @CircuitBreaker(name = ELASTICSEARCH_INSTANCE, fallbackMethod = "revenueAggregationFallback")
    @Retry(name = ELASTICSEARCH_INSTANCE)
    @Bulkhead(name = ELASTICSEARCH_INSTANCE)
    public RevenueReportDto executeRevenueAggregation(String clientId, Instant from, Instant to) {
        return projectionRepository.aggregateRevenueReport(clientId, from, to);
    }

    public RevenueReportDto revenueAggregationFallback(String clientId, Instant from, Instant to, Throwable t) {
        log.error("Circuit breaker open for ES revenue aggregation: clientId={} error={}", clientId, t.getMessage());
        throw new ServiceUnavailableException("Elasticsearch is unavailable. Please try again later.", t);
    }

    public List<TransactionSummaryDto> getTransactions(String clientId, String period) {
        log.info("Transaction list requested: clientId={} period={}", clientId, period);

        Instant[] range = parsePeriod(period);
        List<TransactionProjection> projections =
                projectionRepository.findByClientIdAndTransactedAtBetween(clientId, range[0], range[1]);

        return projections.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    private TransactionSummaryDto toSummaryDto(TransactionProjection p) {
        return TransactionSummaryDto.builder()
                .transactionId(p.getTransactionId())
                .clientId(p.getClientId())
                .status(p.getStatus())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentStatus(p.getPaymentStatus())
                .transactedAt(p.getTransactedAt())
                .region(p.getRegion())
                .build();
    }

    private Instant[] parsePeriod(String period) {
        // Expected format: yyyy-MM (e.g. "2022-01")
        YearMonth ym = YearMonth.parse(period);
        Instant from = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = ym.atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return new Instant[]{from, to};
    }
}
