package com.know_who_how.main_server.global.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_info_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "goal_amount")
    private Long goalAmount;

    @Column(name = "goal_target_date")
    private LocalDate goalTargetDate;

    @Column(name = "expectation_monthly_cost")
    private Long expectationMonthlyCost;

    @Column(name = "fixed_monthly_cost", nullable = false)
    private Long fixedMonthlyCost;

    @Column(name = "retirement_status")
    private Boolean retirementStatus;

    @Column(name = "annual_income", nullable = false)
    private Long annualIncome;

    @Column(name = "target_retired_age", nullable = false)
    private Integer targetRetiredAge;

    @Column(name = "num_dependents", nullable = false)
    private Integer numDependents;

    @Enumerated(EnumType.STRING)
    @Column(name = "mydata_status", nullable = false)
    private MyDataStatus mydataStatus;

    @Builder
    public UserInfo(User user, Long goalAmount, LocalDate goalTargetDate, Long expectationMonthlyCost, Long fixedMonthlyCost, Boolean retirementStatus, Long annualIncome, Integer targetRetiredAge, Integer numDependents, MyDataStatus mydataStatus) {
        this.user = user;
        this.goalAmount = goalAmount;
        this.goalTargetDate = goalTargetDate;
        this.expectationMonthlyCost = expectationMonthlyCost;
        this.fixedMonthlyCost = fixedMonthlyCost;
        this.retirementStatus = retirementStatus;
        this.annualIncome = annualIncome;
        this.targetRetiredAge = targetRetiredAge;
        this.numDependents = numDependents;
        this.mydataStatus = mydataStatus;
    }

    public void updateInfo(Long goalAmount, LocalDate goalTargetDate, Long expectationMonthlyCost, Long fixedMonthlyCost, Boolean retirementStatus, Long annualIncome) {
        this.goalAmount = goalAmount;
        this.goalTargetDate = goalTargetDate;
        this.expectationMonthlyCost = expectationMonthlyCost;
        this.fixedMonthlyCost = fixedMonthlyCost;
        this.retirementStatus = retirementStatus;
        this.annualIncome = annualIncome;
    }

    public enum MyDataStatus {
        CONNECTED, DISCONNECTED, NONE
    }

    // [추가] 마이데이터 상태 업데이트
    public void updateMydataStatus(MyDataStatus status) {
        this.mydataStatus = status;
    }
}
