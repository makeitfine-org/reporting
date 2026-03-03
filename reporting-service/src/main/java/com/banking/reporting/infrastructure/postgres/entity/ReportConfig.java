package com.banking.reporting.infrastructure.postgres.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "report_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "alerts_enabled")
    private boolean alertsEnabled;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
