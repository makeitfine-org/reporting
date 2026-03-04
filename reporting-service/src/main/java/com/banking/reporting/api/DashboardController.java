package com.banking.reporting.api;

import com.banking.reporting.api.dto.DashboardDto;
import com.banking.reporting.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Real-time dashboard endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get real-time dashboard for a client")
    public ResponseEntity<DashboardDto> getDashboard(
            @RequestParam @NotBlank String clientId) {

        log.info("GET /api/reports/dashboard clientId={}", clientId);
        return ResponseEntity.ok(dashboardService.getDashboard(clientId));
    }
}
