package com.banking.transaction.application;

import com.banking.commons.exception.ValidationException;
import com.banking.transaction.api.dto.KycStatusResponse;
import com.banking.transaction.infrastructure.feign.CustomerServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycCheckService {

    private static final String KYC_CACHE_PREFIX = "kyc:";

    private final CustomerServiceClient customerServiceClient;
    private final RedisTemplate<String, String> redisTemplate;

    @CircuitBreaker(name = "customer-service", fallbackMethod = "getKycStatusFromCache")
    @Retry(name = "customer-service")
    public KycStatusResponse getKycStatus(String customerId) {
        KycStatusResponse response = customerServiceClient.getKycStatus(customerId);
        String cacheKey = KYC_CACHE_PREFIX + customerId;
        redisTemplate.opsForValue().set(cacheKey, response.getKycStatus(), Duration.ofMinutes(30));
        return response;
    }

    public KycStatusResponse getKycStatusFromCache(String customerId, Exception ex) {
        log.warn("Customer service unavailable, falling back to cache for customerId={}", customerId);
        String cacheKey = KYC_CACHE_PREFIX + customerId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            throw new ValidationException("KYC status unavailable and no cache found for customer: " + customerId);
        }
        KycStatusResponse fallback = new KycStatusResponse();
        fallback.setCustomerId(customerId);
        fallback.setKycStatus(cached);
        return fallback;
    }

    public void assertKycApproved(String customerId) {
        KycStatusResponse kyc = getKycStatus(customerId);
        if (!"APPROVED".equals(kyc.getKycStatus())) {
            throw new ValidationException("Customer KYC not approved: " + customerId + " status=" + kyc.getKycStatus());
        }
    }
}
