package com.banking.reporting.unit;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.infrastructure.redis.ReportCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportCacheService reportCacheService;

    @Test
    void getFinancialReport_returnsReport_whenCacheExists() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        FinancialReportDto report = new FinancialReportDto();
        when(valueOperations.get(anyString())).thenReturn("json");
        when(objectMapper.readValue(anyString(), eq(FinancialReportDto.class))).thenReturn(report);

        Optional<FinancialReportDto> result = reportCacheService.getFinancialReport("cli-001", "2022-01");

        assertTrue(result.isPresent());
        assertEquals(report, result.get());
    }

    @Test
    void getFinancialReport_returnsEmpty_whenCacheMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<FinancialReportDto> result = reportCacheService.getFinancialReport("cli-001", "2022-01");

        assertFalse(result.isPresent());
    }

    @Test
    void putFinancialReport_setsValueInRedis() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        FinancialReportDto report = new FinancialReportDto();
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        reportCacheService.putFinancialReport("cli-001", "2022-01", report);

        verify(valueOperations).set(anyString(), eq("json"), any(Duration.class));
    }

    @Test
    void getRevenueReport_returnsReport_whenCacheExists() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RevenueReportDto report = new RevenueReportDto();
        when(valueOperations.get(anyString())).thenReturn("json");
        when(objectMapper.readValue(anyString(), eq(RevenueReportDto.class))).thenReturn(report);

        Optional<RevenueReportDto> result = reportCacheService.getRevenueReport("cli-001", "2022-01");

        assertTrue(result.isPresent());
        assertEquals(report, result.get());
    }

    @Test
    void evictClientReports_deletesKeys() {
        Set<String> keys = Set.of("key1", "key2");
        when(redisTemplate.keys(anyString())).thenReturn(keys);

        reportCacheService.evictClientReports("cli-001");

        verify(redisTemplate).delete(keys);
    }

    @Test
    void getFromCache_returnsEmpty_onJsonError() throws JsonProcessingException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("invalid-json");
        when(objectMapper.readValue(anyString(), eq(FinancialReportDto.class)))
                .thenThrow(new JsonProcessingException("error") {});

        Optional<FinancialReportDto> result = reportCacheService.getFinancialReport("cli-001", "2022-01");

        assertFalse(result.isPresent());
    }

    @Test
    void putToCache_handlesJsonError() throws JsonProcessingException {
        // No redisTemplate.opsForValue() called if writeValueAsString fails
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("error") {});

        reportCacheService.putFinancialReport("cli-001", "2022-01", new FinancialReportDto());

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
