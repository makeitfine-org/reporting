package com.banking.reporting.unit;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.application.ReportQueryService;
import com.banking.reporting.domain.exception.ResourceNotFoundException;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import com.banking.reporting.infrastructure.postgres.repository.ReportConfigRepository;
import com.banking.reporting.infrastructure.redis.ReportCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    private static final String CLIENT_ID = "cli-001";
    private static final String PERIOD = "2022-01";
    @Mock
    private TransactionProjectionRepository projectionRepository;
    @Mock
    private ReportConfigRepository reportConfigRepository;
    @Mock
    private ReportCacheService cacheService;
    @InjectMocks
    private ReportQueryService reportQueryService;
    private ReportConfig reportConfig;

    @BeforeEach
    void setUp() {
        reportConfig = ReportConfig.builder()
                .id("cfg-001")
                .clientId(CLIENT_ID)
                .reportType("FINANCIAL")
                .currency("USD")
                .build();
    }

    @Test
    void getFinancialReport_cacheMiss_loadsFromElasticsearch() {
        FinancialReportDto esReport = FinancialReportDto.builder()
                .clientId(CLIENT_ID)
                .totalAmount(BigDecimal.valueOf(50000))
                .totalTransactions(100)
                .completedTransactions(95)
                .build();

        when(cacheService.getFinancialReport(CLIENT_ID, PERIOD)).thenReturn(Optional.empty());
        when(reportConfigRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(reportConfig));
        when(projectionRepository.aggregateFinancialReport(eq(CLIENT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(esReport);

        FinancialReportDto result = reportQueryService.getFinancialReport(CLIENT_ID, PERIOD);

        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(result.getCacheSource()).isEqualTo("ELASTICSEARCH");

        verify(cacheService).putFinancialReport(eq(CLIENT_ID), eq(PERIOD), any());
    }

    @Test
    void getFinancialReport_cacheHit_returnsFromCache() {
        FinancialReportDto cachedReport = FinancialReportDto.builder()
                .clientId(CLIENT_ID)
                .totalAmount(BigDecimal.valueOf(50000))
                .build();

        when(cacheService.getFinancialReport(CLIENT_ID, PERIOD)).thenReturn(Optional.of(cachedReport));

        FinancialReportDto result = reportQueryService.getFinancialReport(CLIENT_ID, PERIOD);

        assertThat(result.getCacheSource()).isEqualTo("REDIS");
        verifyNoInteractions(projectionRepository);
        verifyNoInteractions(reportConfigRepository);
    }

    @Test
    void getFinancialReport_clientNotFound_throwsResourceNotFoundException() {
        when(cacheService.getFinancialReport(CLIENT_ID, PERIOD)).thenReturn(Optional.empty());
        when(reportConfigRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportQueryService.getFinancialReport(CLIENT_ID, PERIOD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(CLIENT_ID);
    }

    @Test
    void getRevenueReport_cacheMiss_loadsFromElasticsearch() {
        RevenueReportDto esReport = RevenueReportDto.builder()
                .clientId(CLIENT_ID)
                .totalRevenue(BigDecimal.valueOf(30000))
                .transactionCount(80)
                .build();

        when(cacheService.getRevenueReport(CLIENT_ID, PERIOD)).thenReturn(Optional.empty());
        when(reportConfigRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(reportConfig));
        when(projectionRepository.aggregateRevenueReport(eq(CLIENT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(esReport);

        RevenueReportDto result = reportQueryService.getRevenueReport(CLIENT_ID, PERIOD);

        assertThat(result.getTotalRevenue()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(result.getCacheSource()).isEqualTo("ELASTICSEARCH");
    }

    @Test
    void getRevenueReport_cacheHit_returnsFromCache() {
        RevenueReportDto cached = RevenueReportDto.builder()
                .clientId(CLIENT_ID)
                .totalRevenue(BigDecimal.valueOf(30000))
                .build();

        when(cacheService.getRevenueReport(CLIENT_ID, PERIOD)).thenReturn(Optional.of(cached));

        RevenueReportDto result = reportQueryService.getRevenueReport(CLIENT_ID, PERIOD);

        assertThat(result.getCacheSource()).isEqualTo("REDIS");
        verifyNoInteractions(reportConfigRepository);
    }

    @Test
    void financialAggregationFallback_throwsServiceUnavailableException() {
        Instant from = Instant.now();
        Instant to = Instant.now();
        RuntimeException ex = new RuntimeException("ES down");

        assertThatThrownBy(() -> reportQueryService.financialAggregationFallback(CLIENT_ID, from, to, ex))
                .isInstanceOf(com.banking.reporting.domain.exception.ServiceUnavailableException.class)
                .hasMessageContaining("Elasticsearch is unavailable");
    }

    @Test
    void revenueAggregationFallback_throwsServiceUnavailableException() {
        Instant from = Instant.now();
        Instant to = Instant.now();
        RuntimeException ex = new RuntimeException("ES down");

        assertThatThrownBy(() -> reportQueryService.revenueAggregationFallback(CLIENT_ID, from, to, ex))
                .isInstanceOf(com.banking.reporting.domain.exception.ServiceUnavailableException.class)
                .hasMessageContaining("Elasticsearch is unavailable");
    }

    @Test
    void getTransactions_returnsProjectionsAsDtos() {
        when(projectionRepository.findByClientIdAndTransactedAtBetween(eq(CLIENT_ID), any(), any()))
                .thenReturn(java.util.List.of(TransactionProjection.builder()
                        .transactionId("tx-001")
                        .clientId(CLIENT_ID)
                        .status("COMPLETED")
                        .build()));

        var result = reportQueryService.getTransactions(CLIENT_ID, PERIOD);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo("tx-001");
    }
}
