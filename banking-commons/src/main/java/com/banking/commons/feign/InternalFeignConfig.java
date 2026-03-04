package com.banking.commons.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class InternalFeignConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return new CorrelationIdRequestInterceptor();
    }

    public static class CorrelationIdRequestInterceptor implements RequestInterceptor {
        @Override
        public void apply(RequestTemplate template) {
            String correlationId = MDC.get(MDC_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            template.header(CORRELATION_ID_HEADER, correlationId);
        }
    }
}
