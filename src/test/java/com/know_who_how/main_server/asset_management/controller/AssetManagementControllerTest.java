package com.know_who_how.main_server.asset_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
import com.know_who_how.main_server.asset_management.dto.RecommendedProductDto;
import com.know_who_how.main_server.asset_management.service.AssetManagementService;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetManagementController.class)
class AssetManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssetManagementService assetManagementService;

    @MockBean
    private User mockUser;

    @BeforeEach
    void setUp() {
        // @AuthenticationPrincipal User user를 모킹하기 위함
        // 실제 User 객체의 필드를 사용해야 하는 경우, mockUser에 해당 필드를 설정해야 합니다.
        // 예: given(mockUser.getInvestmentTendancy()).willReturn(InvestmentTendancy.MODERATE);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(mockUser, ""));
    }

    @Test
    @DisplayName("[성공] 유효한 요청 시, 재무 설문 정보 저장 후 201 Created 반환")
    @WithMockUser
    void savePortfolioInfo_should_returnCreated_when_requestIsValid() throws Exception {
        // given
        PortfolioInfoRequest request = new PortfolioInfoRequest(100000000L, LocalDate.now().plusYears(5), 1000000L, 500000L, false, 50000000L);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/asset-management/info")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(assetManagementService).should().savePortfolioInfo(any(PortfolioInfoRequest.class), any(User.class));
    }

    @Test
    @DisplayName("[실패] 필수 값(goalAmount)이 없는 요청 시, 400 Bad Request 반환")
    @WithMockUser
    void savePortfolioInfo_should_returnBadRequest_when_goalAmountIsMissing() throws Exception {
        // given
        PortfolioInfoRequest invalidRequest = new PortfolioInfoRequest(null, LocalDate.now().plusYears(5), 1000000L, 500000L, false, 50000000L);
        String requestBody = objectMapper.writeValueAsString(invalidRequest);

        // when & then
        mockMvc.perform(post("/api/v1/asset-management/info")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("[성공] 포트폴리오 조회 시, 유효한 PortfolioResponse 반환")
    @WithMockUser
    void getPortfolio_should_returnPortfolio_when_requestIsValid() throws Exception {
        // given
        PortfolioResponse.GoalMetricsDto goalMetrics = new PortfolioResponse.GoalMetricsDto(
                LocalDate.now().plusYears(5), 5, 100000000L, 50000000L, 40000000L, 40
        );
        PortfolioResponse.CashFlowDto cashFlow = new PortfolioResponse.CashFlowDto(
                "월 저축형", 1000000L, null, "KWH 월 저축형 상품", 3.5
        );
        PortfolioResponse.PredictionDto prediction = new PortfolioResponse.PredictionDto(
                "적금 시뮬레이션", 60000000L, 60, 65000000L, 5000000L
        );
        List<RecommendedProductDto> recommendedProducts = List.of(
                new RecommendedProductDto("우리 정기예금", "예적금", "연 3.5%", "우리은행", null)
        );
        PortfolioResponse mockResponse = new PortfolioResponse(goalMetrics, cashFlow, prediction, recommendedProducts);

        given(assetManagementService.getPortfolio(any(User.class))).willReturn(mockResponse);
        given(mockUser.getInvestmentTendancy()).willReturn(InvestmentTendancy.CONSERVATIVE); // Mock User's tendancy

        // when & then
        mockMvc.perform(get("/api/v1/asset-management/portfolio")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.goalMetrics.goalAmount").value(100000000L))
                .andExpect(jsonPath("$.data.recommendedProducts[0].productName").value("우리 정기예금"));

        then(assetManagementService).should().getPortfolio(any(User.class));
    }
}
