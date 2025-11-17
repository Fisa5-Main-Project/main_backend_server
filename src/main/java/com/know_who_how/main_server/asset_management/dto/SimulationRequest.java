package com.know_who_how.main_server.asset_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "재무 시뮬레이션 요청 DTO")
public record SimulationRequest(
    @Schema(description = "예치 또는 월 납입 금액. 1 이상의 값을 입력해야 합니다.", example = "500000")
    @NotNull(message = "금액은 필수입니다.")
    @Min(value = 1, message = "금액은 1 이상이어야 합니다.")
    Long principal,

    @Schema(description = "저축 기간(개월 단위). 1 이상의 값을 입력해야 합니다.", example = "12")
    @NotNull(message = "기간은 필수입니다.")
    @Min(value = 1, message = "기간은 1개월 이상이어야 합니다.")
    Integer periodMonths
) {
}
