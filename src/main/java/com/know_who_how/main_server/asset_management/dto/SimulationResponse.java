package com.know_who_how.main_server.asset_management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "재무 시뮬레이션 응답 DTO")
public class SimulationResponse {
    @Schema(description = "시뮬레이션 타입. '적금 시뮬레이션' 또는 '예금 시뮬레이션'", example = "적금 시뮬레이션")
    private String predictionType;

    @Schema(description = "총 납입 원금 (적금의 경우 '월납입액 * 개월 수')", example = "6000000")
    private Long principal;

    @Schema(description = "시뮬레이션에 적용된 기간(개월 단위)", example = "12")
    private Integer periodMonths;

    @Schema(description = "만기 시 예상 수령액 (원금 + 세후 이자)", example = "6086609")
    private Long expectedAmount;

    @Schema(description = "만기 시 발생하는 세후 이자 금액", example = "86609")
    private Long interestAmount;
}
