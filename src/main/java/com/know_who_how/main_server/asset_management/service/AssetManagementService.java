package com.know_who_how.main_server.asset_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.*;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.FinancialProductRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetManagementService {

    private final UserInfoRepository userInfoRepository;
    private final AssetsRepository assetsRepository;
    private final FinancialProductRepository financialProductRepository;
    private final com.know_who_how.main_server.user.repository.UserRepository userRepository;
    private final com.know_who_how.main_server.user.repository.UserKeywordRepository userKeywordRepository;
    private final ObjectMapper objectMapper;

    // 명세에 따른 상수 정의
    private static final BigDecimal TAX_RATE = new BigDecimal("0.154"); // 이자소득세율 (15.4%)
    private static final BigDecimal SAVINGS_INTEREST_RATE = new BigDecimal("3.15"); // 적금 이자율 (3.15%)
    private static final BigDecimal DEPOSIT_INTEREST_RATE = new BigDecimal("2.80"); // 예금 이자율 (2.80%)
    private static final long SAVINGS_PREDICTION_MONTHLY_DEPOSIT = 500_000L; // 월 저축형 예측 월 납입액
    private static final int PREDICTION_PERIOD_MONTHS = 12; // 예측 기간 (12개월)

    @Transactional
    public void savePortfolioInfo(PortfolioInfoRequest request, User user) {
        User managedUser = userRepository.findById(user.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Optional<UserInfo> optionalUserInfo = userInfoRepository.findByUser(managedUser);

        if (optionalUserInfo.isPresent()) {
            // Update existing info
            UserInfo userInfo = optionalUserInfo.get();
            userInfo.updateInfo(
                request.goalAmount(),
                request.goalTargetDate(),
                request.expectationMonthlyCost(),
                request.fixedMonthlyCost(),
                request.retirementStatus(),
                request.annualIncome()
            );
            userInfoRepository.save(userInfo);
        } else {
            // Create new info
            UserInfo newUserInfo = request.toEntity(managedUser);
            userInfoRepository.save(newUserInfo);
        }
    }

    public PortfolioResponse getPortfolio(User user) {
        UserInfo userInfo = userInfoRepository.findByUser(user)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_INFO_NOT_FOUND));

        List<Asset> assetsList = assetsRepository.findByUser(user);

        PortfolioResponse.GoalMetricsDto goalMetrics = calculateGoalMetrics(userInfo, assetsList);

        // 1. 월간 순저축 여력 계산 (명세에 맞게 수정)
        long monthlyFixedIncome = userInfo.getAnnualIncome() / 12;
        long monthlyNetSavings = monthlyFixedIncome - (userInfo.getExpectationMonthlyCost() + userInfo.getFixedMonthlyCost());

        // 2. 유휴 목돈 계산
        long idleCashAssets = calculateTotalAssetByType(assetsList, AssetType.CURRENT);

        // 3. 저축형/예치형 진단 (상세 로직으로 변경)
        int savingsScore = 0;
        int depositScore = 0;

        // 사용자 키워드 조회
        List<String> userKeywords = userKeywordRepository.findByUser(user).stream()
            .map(userKeyword -> userKeyword.getKeyword().getName())
            .toList();

        // 사용자 나이 계산
        int age = Period.between(user.getBirth(), LocalDate.now()).getYears();

        // 목표 기간 계산
        long yearsToGoal = Period.between(LocalDate.now(), userInfo.getGoalTargetDate()).getYears();

        // 점수 계산
        if (monthlyNetSavings > 300_000) savingsScore++;
        if (userKeywords.contains("목돈 마련") || userKeywords.contains("안정적 생활비")) savingsScore++;
        if (yearsToGoal >= 1 && yearsToGoal <= 5) savingsScore++;

        if (idleCashAssets > 5_000_000) depositScore++;
        if (userKeywords.contains("비상금 확보") || userKeywords.contains("증여/상속")) depositScore++;
        if (age > 50 || userInfo.getRetirementStatus()) depositScore++;

        boolean isSavingsType = savingsScore > depositScore;

        FinancialProduct recommendedMainProduct = financialProductRepository
            .findByProductName(isSavingsType ? "우리 SUPER주거래 적금" : "WON플러스 예금")
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        PortfolioResponse.CashFlowDto cashFlowDiagnostic = createCashFlowDto(isSavingsType, monthlyNetSavings, idleCashAssets, recommendedMainProduct);
        PortfolioResponse.PredictionDto prediction = createPredictionDto(isSavingsType, idleCashAssets);

        return new PortfolioResponse(goalMetrics, cashFlowDiagnostic, prediction);
    }

    public SimulationResponse runInstallmentSavingSimulation(SimulationRequest request) {
        FinancialProduct product = financialProductRepository.findByProductName("우리 SUPER주거래 적금")
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // Use the base rate from the product. Bonus rates are ignored for this simulation.
        BigDecimal interestRate = product.getBaseInterestRate();
        long totalPrincipal = request.principal() * request.periodMonths();

        Map<String, Long> result = calculateInstallmentSavings(request.principal(), interestRate, request.periodMonths());

        return new SimulationResponse(
            "적금 시뮬레이션",
            totalPrincipal,
            request.periodMonths(),
            result.get("expectedAmount"),
            result.get("interestAmount")
        );
    }

    public SimulationResponse runDepositSimulation(SimulationRequest request) {
        FinancialProduct product = financialProductRepository.findByProductName("WON플러스 예금")
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        BigDecimal interestRate = getInterestRateForDeposit(product.getInterestRateDetails(), request.periodMonths());

        Map<String, Long> result = calculateDeposit(request.principal(), interestRate, request.periodMonths());

        return new SimulationResponse(
            "예금 시뮬레이션",
            request.principal(),
            request.periodMonths(),
            result.get("expectedAmount"),
            result.get("interestAmount")
        );
    }

    public FinancialProductResponse getProductDetails(String productName) {
        FinancialProduct product = financialProductRepository.findByProductName(productName)
            .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return new FinancialProductResponse(product);
    }

    private record Tier(int months_gte, int months_lt, double rate) {}
    private record Tiers(List<Tier> tiers) {}

    private BigDecimal getInterestRateForDeposit(String interestRateDetails, int months) {
        if (interestRateDetails == null || interestRateDetails.isBlank()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR); // Or a more specific error
        }
        try {
            Tiers tiers = objectMapper.readValue(interestRateDetails, Tiers.class);
            return tiers.tiers().stream()
                .filter(tier -> months >= tier.months_gte() && months < tier.months_lt())
                .findFirst()
                .map(tier -> BigDecimal.valueOf(tier.rate()))
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR)); // Or rate not found for period
        } catch (JsonProcessingException e) {
            log.error("Failed to parse interest rate details JSON: {}", interestRateDetails, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private PortfolioResponse.GoalMetricsDto calculateGoalMetrics(UserInfo userInfo, List<Asset> assetsList) {
        long totalAsset = userInfo.getUser().getAssetTotal() != null ? userInfo.getUser().getAssetTotal() : 0L;
        long totalLoan = calculateTotalAssetByType(assetsList, AssetType.LOAN);
        long netAsset = totalAsset - totalLoan;

        int goalProgressPercent = (userInfo.getGoalAmount() > 0) ? (int) ((double) netAsset / userInfo.getGoalAmount() * 100) : 0;
        long yearsLeft = Math.max(0, Period.between(LocalDate.now(), userInfo.getGoalTargetDate()).getYears());

        return new PortfolioResponse.GoalMetricsDto(
            userInfo.getGoalTargetDate(), yearsLeft, userInfo.getGoalAmount(),
            totalAsset, netAsset, Math.max(0, Math.min(100, goalProgressPercent))
        );
    }

    private long calculateTotalAssetByType(List<Asset> assetsList, AssetType type) {
        return assetsList.stream()
            .filter(asset -> asset.getType() == type)
            .mapToLong(asset -> asset.getBalance().longValue())
            .sum();
    }

    private PortfolioResponse.CashFlowDto createCashFlowDto(boolean isSavingsType, long monthlyNetSavings, long idleCashAssets, FinancialProduct product) {
        // 명세에 따라 이자율 사용
        double interestRate = isSavingsType ? SAVINGS_INTEREST_RATE.doubleValue() : DEPOSIT_INTEREST_RATE.doubleValue();

        if (isSavingsType) {
            return new PortfolioResponse.CashFlowDto("월 저축형", monthlyNetSavings, null, product.getProductName(), interestRate);
        } else {
            return new PortfolioResponse.CashFlowDto("목돈 예치형", null, idleCashAssets, product.getProductName(), interestRate);
        }
    }

    private PortfolioResponse.PredictionDto createPredictionDto(boolean isSavingsType, long idleCashAssets) {
        if (isSavingsType) {
            // '월 저축형'은 월 50만원, 12개월 적금으로 예측 (명세 기준)
            long principal = SAVINGS_PREDICTION_MONTHLY_DEPOSIT * PREDICTION_PERIOD_MONTHS;
            Map<String, Long> result = calculateInstallmentSavings(SAVINGS_PREDICTION_MONTHLY_DEPOSIT, SAVINGS_INTEREST_RATE, PREDICTION_PERIOD_MONTHS);
            return new PortfolioResponse.PredictionDto("적금 시뮬레이션", principal, PREDICTION_PERIOD_MONTHS, result.get("expectedAmount"), result.get("interestAmount"));
        } else {
            // '목돈 예치형'은 유휴 목돈 전액, 12개월 예금으로 예측 (명세 기준)
            long principal = idleCashAssets;
            Map<String, Long> result = calculateDeposit(principal, DEPOSIT_INTEREST_RATE, PREDICTION_PERIOD_MONTHS);
            return new PortfolioResponse.PredictionDto("예금 시뮬레이션", principal, PREDICTION_PERIOD_MONTHS, result.get("expectedAmount"), result.get("interestAmount"));
        }
    }

    private Map<String, Long> calculateDeposit(long principal, BigDecimal interestRate, int months) {
        BigDecimal p = BigDecimal.valueOf(principal);
        // 연이율을 월이율로 변환하지 않고, 기간을 연으로 변환하여 계산
        BigDecimal r = interestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal m = BigDecimal.valueOf(months);
        BigDecimal monthsInYear = BigDecimal.valueOf(12);

        // 단리 계산: 원금 * 연이율 * (개월수 / 12)
        BigDecimal interest = p.multiply(r).multiply(m.divide(monthsInYear, 10, RoundingMode.HALF_UP));
        BigDecimal interestAfterTax = interest.multiply(BigDecimal.ONE.subtract(TAX_RATE));

        long interestAmount = interestAfterTax.setScale(0, RoundingMode.DOWN).longValue();
        long expectedAmount = principal + interestAmount;

        return Map.of("interestAmount", interestAmount, "expectedAmount", expectedAmount);
    }

    private Map<String, Long> calculateInstallmentSavings(long monthlyDeposit, BigDecimal interestRate, int months) {
        BigDecimal md = BigDecimal.valueOf(monthlyDeposit);
        // 연이율을 월이율로 변환
        BigDecimal r = interestRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = r.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal m = BigDecimal.valueOf(months);

        // 월 단리 적금 계산: 월납입금 * 월이율 * (n * (n+1) / 2)
        BigDecimal interest = md.multiply(monthlyRate)
            .multiply(m.multiply(m.add(BigDecimal.ONE)).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP));

        BigDecimal interestAfterTax = interest.multiply(BigDecimal.ONE.subtract(TAX_RATE));

        long principal = monthlyDeposit * months;
        long interestAmount = interestAfterTax.setScale(0, RoundingMode.DOWN).longValue();
        long expectedAmount = principal + interestAmount;

        return Map.of("interestAmount", interestAmount, "expectedAmount", expectedAmount);
    }
}
