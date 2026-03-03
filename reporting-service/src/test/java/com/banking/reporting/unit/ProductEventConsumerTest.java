package com.banking.reporting.unit;

import com.banking.reporting.infrastructure.elasticsearch.document.ProductDetails;
import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import com.banking.reporting.infrastructure.elasticsearch.repository.TransactionProjectionRepository;
import com.banking.reporting.infrastructure.kafka.consumer.ProductEventConsumer;
import com.banking.reporting.infrastructure.kafka.event.ProductRateUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private TransactionProjectionRepository projectionRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ProductEventConsumer productEventConsumer;

    @Test
    void handleProductRateUpdated_updatesProjections_whenProductIdMatches() {
        ProductRateUpdatedEvent event = ProductRateUpdatedEvent.builder()
                .productId("prod-001")
                .clientId("cli-001")
                .newRate(new BigDecimal("0.05"))
                .build();

        TransactionProjection projection = new TransactionProjection();
        projection.setClientId("cli-001");
        projection.setProductDetails(ProductDetails.builder()
                .productId("prod-001")
                .interestRate(new BigDecimal("0.04"))
                .build());

        when(projectionRepository.findByClientId(eq("cli-001"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        productEventConsumer.handleProductRateUpdated(event, "topic", acknowledgment);

        verify(projectionRepository).save(any(TransactionProjection.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleProductRateUpdated_skipsUpdate_whenProductIdDoesNotMatch() {
        ProductRateUpdatedEvent event = ProductRateUpdatedEvent.builder()
                .productId("prod-002")
                .clientId("cli-001")
                .newRate(new BigDecimal("0.05"))
                .build();

        TransactionProjection projection = new TransactionProjection();
        projection.setClientId("cli-001");
        projection.setProductDetails(ProductDetails.builder()
                .productId("prod-001")
                .interestRate(new BigDecimal("0.04"))
                .build());

        when(projectionRepository.findByClientId(eq("cli-001"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        productEventConsumer.handleProductRateUpdated(event, "topic", acknowledgment);

        verify(projectionRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }
}
