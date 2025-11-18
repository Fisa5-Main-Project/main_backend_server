package com.know_who_how.main_server.asset_management.controller;

import com.know_who_how.main_server.asset_management.dto.*;
import com.know_who_how.main_server.asset_management.service.AssetManagementService;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.entity.User.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asset-management")
@RequiredArgsConstructor
@Tag(name = "3. 자산 관리 API")
public class AssetManagementController {

    private final AssetManagementService assetManagementService;

    @PostMapping("/info")
    @Operation(summary = "재무 설문 정보 저장/수정", description = "인증된 사용자의 재무 목표, 소득 등 설문 정보를 저장하거나 수정합니다. 최초 저장 시 새로운 정보가 생성되고, 이후 호출 시에는 기존 정보가 업데이트됩니다.")
    public ResponseEntity<ApiResponse<Void>> savePortfolioInfo(
            @Valid @RequestBody PortfolioInfoRequest request,
            @AuthenticationPrincipal User user) {
        assetManagementService.savePortfolioInfo(request,
                user);
        return new ResponseEntity<>(ApiResponse.onSuccess(), HttpStatus.CREATED);
    }

    @GetMapping("/portfolio")
    @Operation(summary = "포트폴리오 진단 결과 조회", description = """
        인증된 사용자의 저장된 재무 정보와 MyData 자산 정보를 바탕으로 포트폴리오(목표 달성 현황, 현금 흐름 진단, 미래 자산 예측) 결과를 조회합니다.

        **현금 흐름 진단 (적금형/예치형) 판단 기준:**
        사용자의 특성을 점수화하여 '월 저축형'과 '목돈 예치형' 중 더 적합한 유형을 진단합니다.

        - **월 저축형 점수 항목:**
          - 월 순저축 여력 > 30만원
          - 사용자 목표 키워드에 '목돈 마련' 또는 '안정적 생활비' 포함
          - 재무 목표 기간이 1년 ~ 5년 사이

        - **목돈 예치형 점수 항목:**
          - 유휴 목돈 자산 > 500만원
          - 사용자 목표 키워드에 '비상금 확보' 또는 '증여/상속' 포함
          - 나이가 50세 이상이거나 은퇴 상태

        - **최종 판정:** '월 저축형 점수'가 '목돈 예치형 점수'보다 높으면 '월 저축형'으로, 그 외에는 '목돈 예치형'으로 진단합니다.
        """)
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal User user) {
        PortfolioResponse portfolio = assetManagementService.getPortfolio(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(portfolio));
    }

    @PostMapping("/simulate/saving")
    @Operation(summary = "월 저축(적금) 시뮬레이션", description = "사용자가 입력한 월 납입액과 기간을 바탕으로 '우리 SUPER주거래 적금' 상품의 만기 예상 금액(세후)을 계산하여 반환합니다.")
    public ResponseEntity<ApiResponse<SimulationResponse>> simulateInstallmentSaving(
        @Valid @RequestBody SimulationRequest request) {
        SimulationResponse response = assetManagementService.runInstallmentSavingSimulation(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/simulate/deposit")
    @Operation(summary = "목돈 예치(예금) 시뮬레이션", description = "사용자가 입력한 예치 원금과 기간을 바탕으로 'WON플러스 예금' 상품의 만기 예상 금액(세후)을 계산하여 반환합니다. 기간별 차등 이율이 적용됩니다.")
    public ResponseEntity<ApiResponse<SimulationResponse>> simulateDeposit(
        @Valid @RequestBody SimulationRequest request) {
        SimulationResponse response = assetManagementService.runDepositSimulation(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/products/{productName}")
    @Operation(summary = "금융 상품 상세 정보 조회", description = "상품명을 기준으로 특정 금융 상품의 상세 정보(이자율 등)를 조회합니다. 클라이언트에서 실시간 시뮬레이션을 위한 파라미터를 가져갈 때 사용됩니다.")
    public ResponseEntity<ApiResponse<FinancialProductResponse>> getProductDetails(
        @PathVariable String productName) {
        FinancialProductResponse response = assetManagementService.getProductDetails(productName);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
