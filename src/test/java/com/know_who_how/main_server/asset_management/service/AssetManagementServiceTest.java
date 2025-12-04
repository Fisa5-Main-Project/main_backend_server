package com.know_who_how.main_server.asset_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.UserInfo;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.repository.AssetsRepository;
import com.know_who_how.main_server.user.repository.FinancialProductRepository;
import com.know_who_how.main_server.user.repository.UserInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@MockitoSettings(strictness = Strictness.LENIENT)
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
        @Mock
        private com.know_who_how.main_server.user.repository.UserRepository userRepository;
        @Mock
        private com.know_who_how.main_server.user.repository.UserKeywordRepository userKeywordRepository;
        @Spy
        private ObjectMapper objectMapper;

        @Mock
        private User mockUser;
        @Mock
        private UserInfo mockUserInfo;

        @Test
        @DisplayName("[실패] 포트폴리오 조회 시 사용자 정보가 없으면 예외 발생")
        void getPortfolio_should_throwException_when_userInfoNotFound() {
                given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
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
                given(mockUserInfo.getExpectationMonthlyCost()).willReturn(2000000L); // 월 예상 소비 200만
                given(mockUserInfo.getFixedMonthlyCost()).willReturn(1000000L); // 월 고정비 100만
                given(mockUserInfo.getGoalTargetDate()).willReturn(LocalDate.now().plusYears(6)); // 목표 기간 6년 (저축 점수 감소
                                                                                                  // 목적)
                // 월 순저축 여력 = 400만
                given(mockUserInfo.getUser()).willReturn(mockUser);
                given(mockUser.getBirth()).willReturn(LocalDate.now().minusYears(30)); // 나이 30세
                given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
                given(userInfoRepository.findByUser(mockUser)).willReturn(Optional.of(mockUserInfo));

                // idleCashAssets를 충분히 높게 설정하여 '목돈 예치형'이 되도록
                com.know_who_how.main_server.global.entity.Asset.Asset mockAsset = org.mockito.Mockito
                                .mock(com.know_who_how.main_server.global.entity.Asset.Asset.class);
                given(mockAsset.getType())
                                .willReturn(com.know_who_how.main_server.global.entity.Asset.AssetType.CURRENT);
                given(mockAsset.getBalance()).willReturn(java.math.BigDecimal.valueOf(10000000L)); // 유휴자금 1000만
                given(assetsRepository.findByUser(mockUser)).willReturn(java.util.List.of(mockAsset)); // 부채 0

                given(userKeywordRepository.findByUser(any(User.class))).willReturn(new ArrayList<>()); // 키워드 없음

                String depositProductJson = "{\"tiers\": [{\"months_gte\": 12, \"months_lt\": 13, \"rate\": 2.80}]}";
                FinancialProduct depositProduct = FinancialProduct.builder()
                                .productName("WON플러스 예금").productType(FinancialProduct.ProductType.DEPOSIT)
                                .interestRateDetails(depositProductJson).build();
                given(financialProductRepository.findByProductName("WON플러스 예금"))
                                .willReturn(Optional.of(depositProduct));

                // when
                PortfolioResponse response = assetManagementService.getPortfolio(mockUser);

                // then
                assertThat(response.cashFlowDiagnostic().diagnosticType()).isEqualTo("목돈 예치형");
                assertThat(response.cashFlowDiagnostic().productName()).isEqualTo("WON플러스 예금");
                assertThat(response.prediction().predictionType()).isEqualTo("예금 시뮬레이션");
                assertThat(response.goalMetrics().totalAsset()).isEqualTo(10000000L);
        }

        @Test
        @DisplayName("[성공] 포트폴리오 정보 저장 시 기존 정보가 없으면 새로 생성")
        void savePortfolioInfo_should_createNewInfo_when_userInfoNotFound() {
                // given
                PortfolioInfoRequest request = new PortfolioInfoRequest(1000L, LocalDate.now().plusYears(1), 100L, 50L,
                                false, 5000L, 0);
                given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
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
                PortfolioInfoRequest request = new PortfolioInfoRequest(2000L, LocalDate.now().plusYears(2), 200L, 100L,
                                true, 6000L, 2);
                given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
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
                                request.annualIncome(),
                                request.numDependents());
                then(userInfoRepository).should().save(mockUserInfo);
        }
}
