package com.know_who_how.main_server.asset_management.controller;

import com.know_who_how.main_server.asset_management.dto.PortfolioInfoRequest;
import com.know_who_how.main_server.asset_management.dto.PortfolioResponse;
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
    @Operation(summary = "재무 설문 정보 저장/수정", description = "사용자의 재무 목표, 소득 등 설문 정보를 저장하거나 수정합니다.")
    public ResponseEntity<ApiResponse<Void>> savePortfolioInfo(
            @Valid @RequestBody PortfolioInfoRequest request,
            @AuthenticationPrincipal User user) {
        assetManagementService.savePortfolioInfo(request, user);
        return new ResponseEntity<>(ApiResponse.onSuccess(), HttpStatus.CREATED);
    }

    @GetMapping("/portfolio")
    @Operation(summary = "포트폴리오 진단/예측 조회", description = "저장된 재무 정보와 MyData 자산 정보를 바탕으로 포트폴리오 진단 및 미래 자산 예측 결과를 조회합니다.")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal User user) {
        PortfolioResponse portfolio = assetManagementService.getPortfolio(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(portfolio));
    }
}
