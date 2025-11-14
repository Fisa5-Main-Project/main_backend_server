package com.know_who_how.main_server.asset_management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortfolioResponse(
        GoalMetricsDto goalMetrics,
        CashFlowDto cashFlowDiagnostic,
        PredictionDto prediction
) {
    public record GoalMetricsDto(
            LocalDate goalTargetDate,
            long yearsLeft,
            long goalAmount,
            long totalAsset,
            long netAsset,
            int goalProgressPercent
    ) {}

    public record CashFlowDto(
            String diagnosticType,
            Long monthlyNetSavings,
            Long idleCashAssets,
            String productName,
            double interestRate
    ) {}

    public record PredictionDto(
            String predictionType,
            long principal,
            int periodMonths,
            long expectedAmount,
            long interestAmount
    ) {}
}
