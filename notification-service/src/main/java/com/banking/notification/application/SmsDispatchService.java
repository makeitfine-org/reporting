package com.banking.notification.application;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    private final RestTemplate smsRestTemplate;

    @Value("${sms.gateway.url:http://sms-gateway/send}")
    private String smsGatewayUrl;

    @RateLimiter(name = "sms-dispatch")
    @Retry(name = "sms-dispatch")
    public void sendSms(String phone, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("to", phone, "message", message), headers);
        smsRestTemplate.postForObject(smsGatewayUrl, request, Void.class);
        log.info("SMS sent to={}", phone);
    }
}
