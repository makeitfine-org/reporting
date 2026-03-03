package com.banking.reporting.unit;

import com.banking.reporting.domain.exception.ResourceNotFoundException;
import com.banking.reporting.domain.exception.ServiceUnavailableException;
import com.banking.reporting.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException ex1 = new ResourceNotFoundException("Msg");
        assertThat(ex1.getMessage()).isEqualTo("Msg");

        ResourceNotFoundException ex2 = new ResourceNotFoundException("Type", "Id");
        assertThat(ex2.getMessage()).contains("Type").contains("Id");
    }

    @Test
    void testServiceUnavailableException() {
        ServiceUnavailableException ex1 = new ServiceUnavailableException("Msg");
        assertThat(ex1.getMessage()).isEqualTo("Msg");

        Throwable cause = new RuntimeException("root");
        ServiceUnavailableException ex2 = new ServiceUnavailableException("Msg", cause);
        assertThat(ex2.getMessage()).isEqualTo("Msg");
        assertThat(ex2.getCause()).isEqualTo(cause);
    }

    @Test
    void testValidationException() {
        ValidationException ex = new ValidationException("Msg");
        assertThat(ex.getMessage()).isEqualTo("Msg");
    }
}
