package com.know_who_how.main_server.asset_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.FinancialProductRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AssetManagementServiceTest {

    @InjectMocks
    private AssetManagementService assetManagementService;

    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private AssetsRepository assetsRepository;
    @Mock
    private FinancialProductRepository financialProductRepository;
    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private User mockUser;
    @Mock
    private UserInfo mockUserInfo;

    @BeforeEach
    void setUp() {
        // Inject ObjectMapper into the service instance
        assetManagementService = new AssetManagementService(userInfoRepository, assetsRepository, financialProductRepository, objectMapper);
    }

    @Test
    @DisplayName("[실패] 포트폴리오 조회 시 사용자 정보가 없으면 예외 발생")
    void getPortfolio_should_throwException_when_userInfoNotFound() {
        // given
        given(userInfoRepository.findByUser(any(User.class))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> assetManagementService.getPortfolio(mockUser))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_INFO_NOT_FOUND);
    }

    @Test
    @DisplayName("[성공] 포트폴리오 조회 시 '목돈 예치형' 추천")
    void getPortfolio_should_recommendDeposit_when_idleCashIsSufficient() {
        // given
        given(mockUserInfo.getAnnualIncome()).willReturn(60000000L); // 연봉 6000만
        given(mockUserInfo.getFixedMonthlyCost()).willReturn(1000000L); // 월 고정비 100만
        // 월 순저축 여력 = 400만
        given(userInfoRepository.findByUser(mockUser)).willReturn(Optional.of(mockUserInfo));
        given(assetsRepository.findByUser(mockUser)).willReturn(new ArrayList<>()); // 자산 0
        given(mockUser.getInvestmentTendancy()).willReturn(InvestmentTendancy.CONSERVATIVE);

        String depositProductJson = "{\"tiers\": [{\"months_gte\": 12, \"months_lt\": 13, \"rate\": 2.80}]}";
        FinancialProduct depositProduct = FinancialProduct.builder()
                .productName("WON플러스 예금").productType(FinancialProduct.ProductType.DEPOSIT)
                .interestRateDetails(depositProductJson).build();
        given(financialProductRepository.findByProductName("WON플러스 예금")).willReturn(Optional.of(depositProduct));
        given(financialProductRepository.findAll()).willReturn(List.of(depositProduct));


        // when
        PortfolioResponse response = assetManagementService.getPortfolio(mockUser);

        // then
        assertThat(response.cashFlowDiagnostic().diagnosticType()).isEqualTo("목돈 예치형");
        assertThat(response.cashFlowDiagnostic().productName()).isEqualTo("WON플러스 예금");
        assertThat(response.prediction().predictionType()).isEqualTo("예금 시뮬레이션");
    }


    @Test
    @DisplayName("[성공] 포트폴리오 정보 저장 시 기존 정보가 없으면 새로 생성")
    void savePortfolioInfo_should_createNewInfo_when_userInfoNotFound() {
        // given
        PortfolioInfoRequest request = new PortfolioInfoRequest(1000L, LocalDate.now().plusYears(1), 100L, 50L, false, 5000L);
        given(userInfoRepository.findByUser(any(User.class))).willReturn(Optional.empty());

        // when
        assetManagementService.savePortfolioInfo(request, mockUser);

        // then
        then(userInfoRepository).should().save(any(UserInfo.class));
    }

    @Test
    @DisplayName("[성공] 포트폴리오 정보 저장 시 기존 정보가 있으면 업데이트")
    void savePortfolioInfo_should_updateInfo_when_userInfoExists() {
        // given
        PortfolioInfoRequest request = new PortfolioInfoRequest(2000L, LocalDate.now().plusYears(2), 200L, 100L, true, 6000L);
        given(userInfoRepository.findByUser(any(User.class))).willReturn(Optional.of(mockUserInfo));

        // when
        assetManagementService.savePortfolioInfo(request, mockUser);

        // then
        then(mockUserInfo).should().updateInfo(
                request.goalAmount(),
                request.goalTargetDate(),
                request.expectationMonthlyCost(),
                request.fixedMonthlyCost(),
                request.retirementStatus(),
                request.annualIncome()
        );
        then(userInfoRepository).should().save(mockUserInfo);
    }
}
