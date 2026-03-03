package com.banking.reporting.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfigRequest {

    @NotBlank
    private String clientId;

    @NotBlank
    private String reportType;

    @NotNull
    private String currency;

    private String schedule;
    private boolean alertsEnabled;
    private String timezone;
}
