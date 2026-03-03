package com.banking.reporting.unit;

import com.banking.reporting.api.GlobalExceptionHandler;
import com.banking.reporting.domain.exception.ResourceNotFoundException;
import com.banking.reporting.domain.exception.ServiceUnavailableException;
import com.banking.reporting.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404ProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("ReportConfig", "cli-001");
        ProblemDetail result = handler.handleResourceNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getTitle()).isEqualTo("Resource Not Found");
        assertThat(result.getDetail()).contains("cli-001");
    }

    @Test
    void handleServiceUnavailable_returns503ProblemDetail() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Elasticsearch is down");
        ProblemDetail result = handler.handleServiceUnavailable(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(result.getTitle()).isEqualTo("Service Unavailable");
        assertThat(result.getDetail()).contains("Elasticsearch");
    }

    @Test
    void handleValidation_returns400ProblemDetail() {
        ValidationException ex = new ValidationException("Invalid period format");
        ProblemDetail result = handler.handleValidation(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation Error");
    }

    @Test
    void handleGenericException_returns500ProblemDetail() {
        Exception ex = new RuntimeException("Unexpected error");
        ProblemDetail result = handler.handleGenericException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
    }

    @Test
    void problemDetail_containsTimestamp() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
        ProblemDetail result = handler.handleResourceNotFound(ex);

        assertThat(result.getProperties()).containsKey("timestamp");
    }
}
