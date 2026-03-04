package com.banking.transaction.unit;

import com.banking.commons.exception.ValidationException;
import com.banking.transaction.api.dto.KycStatusResponse;
import com.banking.transaction.application.KycCheckService;
import com.banking.transaction.infrastructure.feign.CustomerServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycCheckServiceTest {

    @Mock
    private CustomerServiceClient customerServiceClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private KycCheckService kycCheckService;

    @Test
    void assertKycApproved_approved_noException() {
        KycStatusResponse resp = new KycStatusResponse();
        resp.setCustomerId("cust-1");
        resp.setKycStatus("APPROVED");

        when(customerServiceClient.getKycStatus("cust-1")).thenReturn(resp);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        assertThatNoException().isThrownBy(() -> kycCheckService.assertKycApproved("cust-1"));
    }

    @Test
    void assertKycApproved_pending_throwsValidationException() {
        KycStatusResponse resp = new KycStatusResponse();
        resp.setCustomerId("cust-2");
        resp.setKycStatus("PENDING");

        when(customerServiceClient.getKycStatus("cust-2")).thenReturn(resp);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> kycCheckService.assertKycApproved("cust-2"))
                .isInstanceOf(ValidationException.class);
    }
}
