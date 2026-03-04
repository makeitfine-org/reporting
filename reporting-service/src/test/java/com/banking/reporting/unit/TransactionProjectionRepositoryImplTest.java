package com.banking.reporting.unit;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProjectionRepositoryImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private TransactionProjectionRepositoryImpl repository;

    @Test
    void aggregateFinancialReport_returnsAggregatedData() {
        String clientId = "cli-001";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        TransactionProjection p1 = new TransactionProjection();
        p1.setAmount(new BigDecimal("100.00"));
        p1.setStatus("COMPLETED");

        TransactionProjection p2 = new TransactionProjection();
        p2.setAmount(new BigDecimal("50.00"));
        p2.setStatus("REVERSED");

        SearchHit<TransactionProjection> hit1 = mock(SearchHit.class);
        when(hit1.getContent()).thenReturn(p1);
        SearchHit<TransactionProjection> hit2 = mock(SearchHit.class);
        when(hit2.getContent()).thenReturn(p2);

        SearchHits<TransactionProjection> searchHits = mock(SearchHits.class);
        when(searchHits.stream()).thenReturn(List.of(hit1, hit2).stream());

        when(elasticsearchOperations.search(any(Query.class), eq(TransactionProjection.class)))
                .thenReturn(searchHits);

        FinancialReportDto report = repository.aggregateFinancialReport(clientId, from, to);

        assertEquals(new BigDecimal("150.00"), report.getTotalAmount());
        assertEquals(2, report.getTotalTransactions());
        assertEquals(1, report.getCompletedTransactions());
        assertEquals(1, report.getReversedTransactions());
    }

    @Test
    void aggregateRevenueReport_returnsAggregatedData() {
        String clientId = "cli-001";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        TransactionProjection p1 = new TransactionProjection();
        p1.setAmount(new BigDecimal("100.00"));
        p1.setStatus("COMPLETED");

        SearchHit<TransactionProjection> hit1 = mock(SearchHit.class);
        when(hit1.getContent()).thenReturn(p1);

        SearchHits<TransactionProjection> searchHits = mock(SearchHits.class);
        when(searchHits.stream()).thenReturn(List.of(hit1).stream());

        when(elasticsearchOperations.search(any(Query.class), eq(TransactionProjection.class)))
                .thenReturn(searchHits);

        RevenueReportDto report = repository.aggregateRevenueReport(clientId, from, to);

        assertEquals(new BigDecimal("100.00"), report.getTotalRevenue());
        assertEquals(1, report.getTransactionCount());
        assertEquals(new BigDecimal("100.00"), report.getAverageTransactionValue());
    }

    @Test
    void aggregateRevenueReport_returnsZero_whenNoHits() {
        String clientId = "cli-001";
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();

        SearchHits<TransactionProjection> searchHits = mock(SearchHits.class);
        when(searchHits.stream()).thenReturn(Collections.<SearchHit<TransactionProjection>>emptyList().stream());

        when(elasticsearchOperations.search(any(Query.class), eq(TransactionProjection.class)))
                .thenReturn(searchHits);

        RevenueReportDto report = repository.aggregateRevenueReport(clientId, from, to);

        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertEquals(0, report.getTransactionCount());
        assertEquals(BigDecimal.ZERO, report.getAverageTransactionValue());
    }
}
