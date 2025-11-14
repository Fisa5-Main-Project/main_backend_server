package com.know_who_how.main_server.asset_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
import com.know_who_how.main_server.asset_management.dto.RecommendedProductDto;
import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.FinancialProductRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetManagementService {

    private final UserInfoRepository userInfoRepository;
    private final AssetsRepository assetsRepository;
    private final FinancialProductRepository financialProductRepository;
    private final ObjectMapper objectMapper;

    private static final double TAX_RATE = 0.154; // 이자소득세율

    @Transactional
    public void savePortfolioInfo(PortfolioInfoRequest request, User user) {
        Optional<UserInfo> optionalUserInfo = userInfoRepository.findByUser(user);

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
            userInfoRepository.save(userInfo); // JPA의 Dirty Checking으로 인해 사실상 불필요하지만, 명시성을 위해 유지
        } else {
            // Create new info
            UserInfo newUserInfo = request.toEntity(user);
            userInfoRepository.save(newUserInfo);
        }
    }

    public PortfolioResponse getPortfolio(User user) {
        UserInfo userInfo = userInfoRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_INFO_NOT_FOUND));

        List<Asset> assetsList = assetsRepository.findByUser(user);

        PortfolioResponse.GoalMetricsDto goalMetrics = calculateGoalMetrics(userInfo, assetsList);

        long monthlyFixedIncome = userInfo.getAnnualIncome() / 12;
        long monthlyNetSavings = monthlyFixedIncome - userInfo.getFixedMonthlyCost();
        long idleCashAssets = calculateTotalAssetByType(assetsList, AssetType.CURRENT);

        boolean isSavingsType = monthlyNetSavings > idleCashAssets * 0.1;

        FinancialProduct recommendedMainProduct = financialProductRepository
                .findByProductName(isSavingsType ? "WON 적금" : "WON플러스 예금")
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        PortfolioResponse.CashFlowDto cashFlowDiagnostic = createCashFlowDto(isSavingsType, monthlyNetSavings, idleCashAssets, recommendedMainProduct);
        PortfolioResponse.PredictionDto prediction = createPredictionDto(isSavingsType, monthlyNetSavings, idleCashAssets, goalMetrics.yearsLeft(), recommendedMainProduct);
        List<RecommendedProductDto> recommendedProducts = getRecommendedProducts(user.getInvestmentTendancy());

        return new PortfolioResponse(goalMetrics, cashFlowDiagnostic, prediction, recommendedProducts);
    }

    private List<RecommendedProductDto> getRecommendedProducts(InvestmentTendancy tendancy) {
        // TODO: 현재는 모든 상품을 보여주지만, 향후 investmentTendancy를 사용하여 필터링해야 합니다.
        return financialProductRepository.findAll().stream()
                .map(fp -> new RecommendedProductDto(
                        fp.getProductName(),
                        fp.getProductType().toString(),
                        (fp.getBaseInterestRate() != null ? "연 " + fp.getBaseInterestRate() + "%" : "기간별 차등"),
                        fp.getBankName(),
                        null))
                .collect(Collectors.toList());
    }

    private PortfolioResponse.GoalMetricsDto calculateGoalMetrics(UserInfo userInfo, List<Asset> assetsList) {
        long totalAsset = assetsList.stream().map(Asset::getBalance).mapToLong(BigInteger::longValue).sum();
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
        BigDecimal interestRate = getApplicableInterestRate(product, 12); // 12개월 기준 금리
        if (isSavingsType) {
            return new PortfolioResponse.CashFlowDto("월 저축형", monthlyNetSavings, null, product.getProductName(), interestRate.doubleValue());
        } else {
            return new PortfolioResponse.CashFlowDto("목돈 예치형", null, idleCashAssets, product.getProductName(), interestRate.doubleValue());
        }
    }

    private PortfolioResponse.PredictionDto createPredictionDto(boolean isSavingsType, long monthlyNetSavings, long idleCashAssets, long yearsLeft, FinancialProduct product) {
        int periodMonths = (int) (yearsLeft * 12);
        if (periodMonths <= 0) periodMonths = 12;

        if (isSavingsType) {
            long depositAmount = Math.min(monthlyNetSavings, product.getMaxAmount()); // 월 최대 납입액 제약조건 반영
            long principal = depositAmount * periodMonths;
            Map<String, Long> result = calculateInstallmentSavingsSimple(depositAmount, product, periodMonths);
            return new PortfolioResponse.PredictionDto("적금 시뮬레이션", principal, periodMonths, result.get("expectedAmount"), result.get("interestAmount"));
        } else {
            long principal = idleCashAssets;
            Map<String, Long> result = calculateDepositSimple(principal, product, periodMonths);
            return new PortfolioResponse.PredictionDto("예금 시뮬레이션", principal, periodMonths, result.get("expectedAmount"), result.get("interestAmount"));
        }
    }

    private Map<String, Long> calculateDepositSimple(long principal, FinancialProduct product, int months) {
        BigDecimal applicableRate = getApplicableInterestRate(product, months);
        BigDecimal p = BigDecimal.valueOf(principal);
        BigDecimal r = applicableRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal m = BigDecimal.valueOf(months);
        BigDecimal monthsInYear = BigDecimal.valueOf(12);

        BigDecimal interest = p.multiply(r).multiply(m.divide(monthsInYear, 10, RoundingMode.HALF_UP));
        BigDecimal interestAfterTax = interest.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(TAX_RATE)));

        long interestAmount = interestAfterTax.setScale(0, RoundingMode.DOWN).longValue();
        long expectedAmount = principal + interestAmount;

        return Map.of("interestAmount", interestAmount, "expectedAmount", expectedAmount);
    }

    private Map<String, Long> calculateInstallmentSavingsSimple(long monthlyDeposit, FinancialProduct product, int months) {
        BigDecimal md = BigDecimal.valueOf(monthlyDeposit);
        BigDecimal r = product.getBaseInterestRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal m = BigDecimal.valueOf(months);
        BigDecimal monthsInYear = BigDecimal.valueOf(12);

        BigDecimal interest = md.multiply(r.divide(monthsInYear, 10, RoundingMode.HALF_UP))
                .multiply(m.multiply(m.add(BigDecimal.ONE)).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP));

        BigDecimal interestAfterTax = interest.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(TAX_RATE)));

        long principal = monthlyDeposit * months;
        long interestAmount = interestAfterTax.setScale(0, RoundingMode.DOWN).longValue();
        long expectedAmount = principal + interestAmount;

        return Map.of("interestAmount", interestAmount, "expectedAmount", expectedAmount);
    }

    private BigDecimal getApplicableInterestRate(FinancialProduct product, int months) {
        if (product.getProductType() == FinancialProduct.ProductType.DEPOSIT && product.getInterestRateDetails() != null) {
            try {
                JsonNode root = objectMapper.readTree(product.getInterestRateDetails());
                JsonNode tiers = root.path("tiers");
                return StreamSupport.stream(tiers.spliterator(), false)
                        .filter(tier -> months >= tier.get("months_gte").asInt() && months < tier.get("months_lt").asInt())
                        .findFirst()
                        .map(tier -> BigDecimal.valueOf(tier.get("rate").asDouble()))
                        .orElse(product.getBaseInterestRate() != null ? product.getBaseInterestRate() : BigDecimal.ZERO);
            } catch (JsonProcessingException e) {
                log.error("금리 정보 JSON 파싱 실패: {}", product.getProductName(), e);
                return BigDecimal.ZERO;
            }
        }
        return product.getBaseInterestRate() != null ? product.getBaseInterestRate() : BigDecimal.ZERO;
    }
}
