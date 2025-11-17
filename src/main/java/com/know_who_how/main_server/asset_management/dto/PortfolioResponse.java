package com.know_who_how.main_server.asset_management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "포트폴리오 진단 결과 전체 응답 DTO")
public record PortfolioResponse(
        @Schema(description = "사용자의 재무 목표 달성 현황 관련 지표")
        GoalMetricsDto goalMetrics,
        @Schema(description = "사용자의 월간 현금 흐름 진단 및 추천 상품 정보")
        CashFlowDto cashFlowDiagnostic,
        @Schema(description = "진단 결과에 따른 기본 미래 자산 예측 시뮬레이션")
        PredictionDto prediction
) {
    @Schema(description = "재무 목표 현황 지표 DTO")
    public record GoalMetricsDto(
            @Schema(description = "사용자가 설정한 재무 목표 달성일", example = "2030-12-31")
            LocalDate goalTargetDate,
            @Schema(description = "목표일까지 남은 기간 (년 단위)", example = "6")
            long yearsLeft,
            @Schema(description = "사용자가 설정한 재무 목표 금액", example = "100000000")
            long goalAmount,
            @Schema(description = "연동된 MyData 기준 총자산", example = "50000000")
            long totalAsset,
            @Schema(description = "순자산 (총자산 - 총부채)", example = "45000000")
            long netAsset,
            @Schema(description = "목표 달성률 (순자산 / 목표금액 * 100)", example = "45")
            int goalProgressPercent
    ) {}

    @Schema(description = "현금 흐름 진단 DTO")
    public record CashFlowDto(
            @Schema(description = "진단 타입. '월 저축형' 또는 '목돈 예치형'으로 구분됩니다.", example = "월 저축형")
            String diagnosticType,
            @Schema(description = "월간 순저축 여력 (월 소득 - 월 지출). '월 저축형'일 경우에만 값이 있습니다.", example = "1000000")
            Long monthlyNetSavings,
            @Schema(description = "즉시 투입 가능한 유휴 목돈 자산. '목돈 예치형'일 경우에만 값이 있습니다.", example = "20000000")
            Long idleCashAssets,
            @Schema(description = "진단 타입에 따라 추천된 금융 상품명", example = "우리 SUPER주거래 적금")
            String productName,
            @Schema(description = "추천 상품의 기본 이자율 (%)", example = "3.15")
            double interestRate
    ) {}

    @Schema(description = "기본 미래 자산 예측 DTO")
    public record PredictionDto(
            @Schema(description = "예측 타입. '적금 시뮬레이션' 또는 '예금 시뮬레이션'으로 구분됩니다.", example = "적금 시뮬레이션")
            String predictionType,
            @Schema(description = "시뮬레이션에 사용된 원금. 적금은 월 50만원 * 12개월, 예금은 유휴 목돈 기준입니다.", example = "6000000")
            long principal,
            @Schema(description = "기본 예측 기간 (12개월)", example = "12")
            int periodMonths,
            @Schema(description = "만기 시 예상 수령액 (원금 + 세후 이자)", example = "6102325")
            long expectedAmount,
            @Schema(description = "예상되는 세후 이자 금액", example = "102325")
            long interestAmount
    ) {}
}
