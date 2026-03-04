package com.banking.notification.unit;

import com.banking.notification.application.SmsDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsDispatchServiceTest {

    @Mock
    private RestTemplate smsRestTemplate;

    @InjectMocks
    private SmsDispatchService smsDispatchService;

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_success_postsToGateway() {
        // smsGatewayUrl is null under @InjectMocks (no @Value injection)
        smsDispatchService.sendSms("+49123456789", "Your transaction has been processed.");

        // nullable(String.class) forces the String overload instead of the URI overload
        verify(smsRestTemplate).postForObject(nullable(String.class), any(HttpEntity.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_restTemplateThrows_propagatesException() {
        doThrow(new ResourceAccessException("Connection refused"))
                .when(smsRestTemplate).postForObject(nullable(String.class), any(HttpEntity.class), any());

        assertThatThrownBy(() -> smsDispatchService.sendSms("+49123456789", "message"))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Connection refused");
    }
}
