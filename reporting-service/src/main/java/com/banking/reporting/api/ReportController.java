package com.banking.reporting.api;

import com.banking.reporting.api.dto.FinancialReportDto;
import com.banking.reporting.api.dto.ReportConfigRequest;
import com.banking.reporting.api.dto.RevenueReportDto;
import com.banking.reporting.api.dto.TransactionSummaryDto;
import com.banking.reporting.application.ReportQueryService;
import com.banking.reporting.infrastructure.postgres.entity.ReportConfig;
import com.banking.reporting.infrastructure.postgres.repository.ReportConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Financial reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportQueryService reportQueryService;
    private final ReportConfigRepository reportConfigRepository;

    @GetMapping("/financial")
    @Operation(summary = "Get monthly financial report")
    @PreAuthorize("hasAnyRole('ANALYST', 'BI_SERVICE', 'ADMIN')")
    public ResponseEntity<FinancialReportDto> getFinancialReport(
            @Parameter(description = "Client ID", required = true)
            @RequestParam @NotBlank String clientId,
            @Parameter(description = "Period in yyyy-MM format", required = true)
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}") String period) {

        log.info("GET /api/reports/financial clientId={} period={}", clientId, period);
        return ResponseEntity.ok(reportQueryService.getFinancialReport(clientId, period));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get monthly revenue report")
    @PreAuthorize("hasAnyRole('ANALYST', 'BI_SERVICE', 'ADMIN')")
    public ResponseEntity<RevenueReportDto> getRevenueReport(
            @RequestParam @NotBlank String clientId,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}") String period) {

        log.info("GET /api/reports/revenue clientId={} period={}", clientId, period);
        return ResponseEntity.ok(reportQueryService.getRevenueReport(clientId, period));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction list for a period")
    @PreAuthorize("hasAnyRole('ANALYST', 'BI_SERVICE', 'ADMIN')")
    public ResponseEntity<List<TransactionSummaryDto>> getTransactions(
            @RequestParam @NotBlank String clientId,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}") String period) {

        log.info("GET /api/reports/transactions clientId={} period={}", clientId, period);
        return ResponseEntity.ok(reportQueryService.getTransactions(clientId, period));
    }

    @PostMapping("/config")
    @Operation(summary = "Create or update report configuration for a client")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ReportConfig> createReportConfig(
            @Valid @RequestBody ReportConfigRequest request) {

        log.info("POST /api/reports/config clientId={}", request.getClientId());

        ReportConfig config = reportConfigRepository.findByClientId(request.getClientId())
                .orElse(new ReportConfig());

        config.setClientId(request.getClientId());
        config.setReportType(request.getReportType());
        config.setCurrency(request.getCurrency());
        config.setTimezone(request.getTimezone());
        config.setAlertsEnabled(request.isAlertsEnabled());

        ReportConfig saved = reportConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }
}
