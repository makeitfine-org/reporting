package com.banking.reporting.infrastructure.elasticsearch.repository;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class TransactionProjectionRepositoryImpl implements TransactionProjectionRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public FinancialReportDto aggregateFinancialReport(String clientId, Instant from, Instant to) {
        log.debug("Aggregating financial report for clientId={} from={} to={}", clientId, from, to);

        Criteria criteria = new Criteria("clientId").is(clientId)
                .and(new Criteria("transactedAt").between(from, to));
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<TransactionProjection> hits =
                elasticsearchOperations.search(query, TransactionProjection.class);

        List<TransactionProjection> projections = hits.stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());

        BigDecimal totalAmount = projections.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransactions = projections.size();
        long completedTransactions = projections.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .count();
        long reversedTransactions = projections.stream()
                .filter(p -> "REVERSED".equals(p.getStatus()))
                .count();
        long chargebacks = projections.stream()
                .filter(p -> "CHARGEBACK".equals(p.getStatus()))
                .count();

        return FinancialReportDto.builder()
                .clientId(clientId)
                .periodFrom(from)
                .periodTo(to)
                .totalAmount(totalAmount)
                .totalTransactions(totalTransactions)
                .completedTransactions(completedTransactions)
                .reversedTransactions(reversedTransactions)
                .chargebacks(chargebacks)
                .build();
    }

    @Override
    public RevenueReportDto aggregateRevenueReport(String clientId, Instant from, Instant to) {
        log.debug("Aggregating revenue report for clientId={} from={} to={}", clientId, from, to);

        Criteria criteria = new Criteria("clientId").is(clientId)
                .and(new Criteria("transactedAt").between(from, to))
                .and(new Criteria("status").is("COMPLETED"));
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<TransactionProjection> hits =
                elasticsearchOperations.search(query, TransactionProjection.class);

        List<TransactionProjection> projections = hits.stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());

        BigDecimal totalRevenue = projections.stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageTransactionValue = projections.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(projections.size()), 2, java.math.RoundingMode.HALF_UP);

        return RevenueReportDto.builder()
                .clientId(clientId)
                .periodFrom(from)
                .periodTo(to)
                .totalRevenue(totalRevenue)
                .transactionCount(projections.size())
                .averageTransactionValue(averageTransactionValue)
                .build();
    }
}
