package com.know_who_how.main_server.asset_management.dto;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PortfolioInfoRequest(
        @NotNull(message = "목표 금액은 필수입니다.")
        @Min(value = 0, message = "목표 금액은 0 이상이어야 합니다.")
        Long goalAmount,

        @NotNull(message = "목표 달성일은 필수입니다.")
        @FutureOrPresent(message = "목표 달성일은 현재 또는 미래여야 합니다.")
        LocalDate goalTargetDate,

        @NotNull(message = "예상 월 소비액은 필수입니다.")
        @Min(value = 0, message = "예상 월 소비액은 0 이상이어야 합니다.")
        Long expectationMonthlyCost,

        @NotNull(message = "월 고정비는 필수입니다.")
        @Min(value = 0, message = "월 고정비는 0 이상이어야 합니다.")
        Long fixedMonthlyCost,

        @NotNull(message = "은퇴 여부는 필수입니다.")
        Boolean retirementStatus,

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
