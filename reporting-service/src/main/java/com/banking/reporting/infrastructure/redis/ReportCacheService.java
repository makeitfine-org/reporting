package com.banking.reporting.infrastructure.redis;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCacheService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String FINANCIAL_PREFIX = "report:financial:";
    private static final String REVENUE_PREFIX = "report:revenue:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<FinancialReportDto> getFinancialReport(String clientId, String period) {
        String key = FINANCIAL_PREFIX + clientId + ":" + period;
        return getFromCache(key, FinancialReportDto.class);
    }

    public void putFinancialReport(String clientId, String period, FinancialReportDto report) {
        String key = FINANCIAL_PREFIX + clientId + ":" + period;
        putToCache(key, report);
    }

    public Optional<RevenueReportDto> getRevenueReport(String clientId, String period) {
        String key = REVENUE_PREFIX + clientId + ":" + period;
        return getFromCache(key, RevenueReportDto.class);
    }

    public void putRevenueReport(String clientId, String period, RevenueReportDto report) {
        String key = REVENUE_PREFIX + clientId + ":" + period;
        putToCache(key, report);
    }

    public void evictClientReports(String clientId) {
        log.info("Evicting cache entries for clientId={}", clientId);
        var keys = redisTemplate.keys("report:*:" + clientId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private <T> Optional<T> getFromCache(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached value for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void putToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value for cache key={}: {}", key, e.getMessage());
        }
    }
}
