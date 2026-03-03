package com.banking.reporting.infrastructure.kafka.event;

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
    private int retryCount;
    private Instant failedAt;
}
