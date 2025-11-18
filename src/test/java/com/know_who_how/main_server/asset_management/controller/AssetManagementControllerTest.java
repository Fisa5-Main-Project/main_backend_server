package com.know_who_how.main_server.asset_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
import com.know_who_how.main_server.asset_management.service.AssetManagementService;
import com.know_who_how.main_server.global.entity.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
    private com.know_who_how.main_server.global.jwt.JwtUtil jwtUtil;

    @MockBean
    private com.know_who_how.main_server.global.util.RedisUtil redisUtil;

    @Test
    @DisplayName("[성공] 유효한 요청 시, 재무 설문 정보 저장 후 201 Created 반환")
    void savePortfolioInfo_should_returnCreated_when_requestIsValid() throws Exception {
        // given
        User mockUser = org.mockito.Mockito.mock(User.class);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mockUser, "", java.util.Collections.emptyList());

        PortfolioInfoRequest request = new PortfolioInfoRequest(100000000L, LocalDate.now().plusYears(5), 1000000L, 500000L, false, 50000000L);
        String requestBody = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/asset-management/info")
                        .with(authentication(authentication))
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
    void savePortfolioInfo_should_returnBadRequest_when_goalAmountIsMissing() throws Exception {
        // given
        User mockUser = org.mockito.Mockito.mock(User.class);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mockUser, "", java.util.Collections.emptyList());

        PortfolioInfoRequest invalidRequest = new PortfolioInfoRequest(null, LocalDate.now().plusYears(5), 1000000L, 500000L, false, 50000000L);
        String requestBody = objectMapper.writeValueAsString(invalidRequest);

        // when & then
        mockMvc.perform(post("/api/v1/asset-management/info")
                        .with(authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("[성공] 포트폴리오 조회 시, 유효한 PortfolioResponse 반환")
    void getPortfolio_should_returnPortfolio_when_requestIsValid() throws Exception {
        // given
        User mockUser = org.mockito.Mockito.mock(User.class);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mockUser, "", java.util.Collections.emptyList());

        PortfolioResponse.GoalMetricsDto goalMetrics = new PortfolioResponse.GoalMetricsDto(
                LocalDate.now().plusYears(5), 5, 100000000L, 50000000L, 40000000L, 40
        );
        PortfolioResponse.CashFlowDto cashFlow = new PortfolioResponse.CashFlowDto(
                "월 저축형", 1000000L, null, "은행 월 저축형 상품", 3.5
        );
        PortfolioResponse.PredictionDto prediction = new PortfolioResponse.PredictionDto(
                "적금 시뮬레이션", 60000000L, 60, 65000000L, 5000000L
        );
        PortfolioResponse mockResponse = new PortfolioResponse(goalMetrics, cashFlow, prediction);

        given(assetManagementService.getPortfolio(any(User.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/asset-management/portfolio")
                        .with(authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.goalMetrics.goalAmount").value(100000000L))
                .andExpect(jsonPath("$.data.cashFlowDiagnostic.productName").value("은행 월 저축형 상품"));

        then(assetManagementService).should().getPortfolio(any(User.class));
    }
}
