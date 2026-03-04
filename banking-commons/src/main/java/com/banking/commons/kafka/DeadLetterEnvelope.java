package com.banking.commons.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEnvelope {

    private String originalTopic;
    private String originalKey;
    private Object originalPayload;
    private String errorMessage;
    private String errorClass;
    private int attemptCount;
    private Instant failedAt;
    private String serviceName;

    public static DeadLetterEnvelope of(String originalTopic, Object payload,
                                        Throwable error, int attempts, String service) {
        return DeadLetterEnvelope.builder()
                .originalTopic(originalTopic)
                .originalPayload(payload)
                .errorMessage(error.getMessage())
                .errorClass(error.getClass().getSimpleName())
                .attemptCount(attempts)
                .failedAt(Instant.now())
                .serviceName(service)
                .build();
    }
}
