package com.banking.reporting.unit;

import com.banking.reporting.api.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter filter;

    @Test
    void doFilterInternal_existingCorrelationId_propagatesIt() throws Exception {
        String existingId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-Id")).thenReturn(existingId);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Correlation-Id", existingId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_missingCorrelationId_generatesNewOne() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-Id"), headerCaptor.capture());

        String generatedId = headerCaptor.getValue();
        assertThat(generatedId).isNotBlank();
        assertThat(generatedId).matches("[0-9a-f-]{36}"); // UUID format
    }

    @Test
    void doFilterInternal_blankCorrelationId_generatesNewOne() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("  ");

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-Id"), headerCaptor.capture());
        assertThat(headerCaptor.getValue()).isNotBlank();
    }
}
