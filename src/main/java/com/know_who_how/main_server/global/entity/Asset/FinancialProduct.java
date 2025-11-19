package com.know_who_how.main_server.global.entity.Asset;

import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "financial_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_name", nullable = false, unique = true)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType; // DEPOSIT(예금), SAVINGS(적금)

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "min_amount")
    private Long minAmount;

    @Column(name = "max_amount")
    private Long maxAmount; // 예: 적금의 월 최대 납입액

    @Column(name = "min_period_months")
    private Integer minPeriodMonths;

    @Column(name = "max_period_months")
    private Integer maxPeriodMonths;

    @Column(name = "base_interest_rate", precision = 5, scale = 2)
    private BigDecimal baseInterestRate; // 기본 금리 (단일 금리 상품용)

    @Lob
    @Column(name = "interest_rate_details", columnDefinition = "TEXT")
    private String interestRateDetails; // JSON 형태의 기간별 차등 금리 정보

    @Lob
    @Column(name = "bonus_rate_details", columnDefinition = "TEXT")
    private String bonusRateDetails; // JSON 형태의 우대 금리 정보

    @Enumerated(EnumType.STRING)
    @Column(name = "compounding_strategy", nullable = false)
    private CompoundingStrategy compoundingStrategy; // SIMPLE(단리), MONTHLY(월복리) 등

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_tendency")
    private InvestmentTendancy applicableTendency; // 추천을 위한 투자 성향

    public enum ProductType {
        DEPOSIT, SAVINGS
    }

    public enum CompoundingStrategy {
        SIMPLE, MONTHLY, ANNUALLY
    }

    @Builder
    public FinancialProduct(String productName, ProductType productType, String bankName, Long minAmount, Long maxAmount, Integer minPeriodMonths, Integer maxPeriodMonths, BigDecimal baseInterestRate, String interestRateDetails, String bonusRateDetails, CompoundingStrategy compoundingStrategy, InvestmentTendancy applicableTendency) {
        this.productName = productName;
        this.productType = productType;
        this.bankName = bankName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minPeriodMonths = minPeriodMonths;
        this.maxPeriodMonths = maxPeriodMonths;
        this.baseInterestRate = baseInterestRate;
        this.interestRateDetails = interestRateDetails;
        this.bonusRateDetails = bonusRateDetails;
        this.compoundingStrategy = compoundingStrategy;
        this.applicableTendency = applicableTendency;
    }
}
