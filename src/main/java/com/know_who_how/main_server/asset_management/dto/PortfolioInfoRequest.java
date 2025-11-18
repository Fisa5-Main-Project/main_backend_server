package com.know_who_how.main_server.asset_management.dto;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "재무 설문 정보 저장/수정 요청 DTO")
public record PortfolioInfoRequest(
        @Schema(description = "재무 목표 금액. 0 이상의 값을 입력해야 합니다.", example = "100000000")
        @NotNull(message = "목표 금액은 필수입니다.")
        @Min(value = 0, message = "목표 금액은 0 이상이어야 합니다.")
        Long goalAmount,

        @Schema(description = "재무 목표 달성일. 현재 또는 미래 날짜여야 합니다.", example = "2030-12-31")
        @NotNull(message = "목표 달성일은 필수입니다.")
        @FutureOrPresent(message = "목표 달성일은 현재 또는 미래여야 합니다.")
        LocalDate goalTargetDate,

        @Schema(description = "월 예상 생활비(소비액). 0 이상의 값을 입력해야 합니다.", example = "1500000")
        @NotNull(message = "예상 월 소비액은 필수입니다.")
        @Min(value = 0, message = "예상 월 소비액은 0 이상이어야 합니다.")
        Long expectationMonthlyCost,

        @Schema(description = "월 고정 지출비 (대출이자, 월세 등). 0 이상의 값을 입력해야 합니다.", example = "500000")
        @NotNull(message = "월 고정비는 필수입니다.")
        @Min(value = 0, message = "월 고정비는 0 이상이어야 합니다.")
        Long fixedMonthlyCost,

        @Schema(description = "현재 은퇴 여부. 은퇴했다면 true, 아니면 false.", example = "false")
        @NotNull(message = "은퇴 여부는 필수입니다.")
        Boolean retirementStatus,

        @Schema(description = "연소득. 0 이상의 값을 입력해야 합니다.", example = "60000000")
        @NotNull(message = "연소득은 필수입니다.")
        @Min(value = 0, message = "연소득은 0 이상이어야 합니다.")
        Long annualIncome
) {
    public UserInfo toEntity(User user) {
        return UserInfo.builder()
                .user(user)
                .goalAmount(this.goalAmount)
                .goalTargetDate(this.goalTargetDate)
                .expectationMonthlyCost(this.expectationMonthlyCost)
                .fixedMonthlyCost(this.fixedMonthlyCost)
                .retirementStatus(this.retirementStatus)
                .annualIncome(this.annualIncome)
                .targetRetiredAge(0) // 기본값
                .numDependents(0)    // 기본값
                .mydataStatus(UserInfo.MyDataStatus.NONE) // 기본값
                .build();
    }
}
