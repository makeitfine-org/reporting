package com.banking.reporting.infrastructure.elasticsearch.repository;

import com.banking.reporting.infrastructure.elasticsearch.document.TransactionProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionProjectionRepository
        extends ElasticsearchRepository<TransactionProjection, String>,
                TransactionProjectionRepositoryCustom {

    Page<TransactionProjection> findByClientId(String clientId, Pageable pageable);

    List<TransactionProjection> findByClientIdAndTransactedAtBetween(
            String clientId, Instant from, Instant to);

    long countByClientIdAndStatus(String clientId, String status);
}
