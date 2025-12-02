package com.know_who_how.main_server.admin.controller;

import com.know_who_how.main_server.admin.dto.*;
import com.know_who_how.main_server.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<List<StatCardResponseDto>> getStatCardsData() {
        return ResponseEntity.ok(adminDashboardService.getStatCardsData());
    }

    @GetMapping("/dashboard/user-growth")
    public ResponseEntity<List<UserGrowthResponseDto>> getUserGrowthData() {
        return ResponseEntity.ok(adminDashboardService.getUserGrowthData());
    }

    @GetMapping("/dashboard/dau")
    public ResponseEntity<List<UserGrowthResponseDto>> getDauData() {
        return ResponseEntity.ok(adminDashboardService.getDauData());
    }

    @GetMapping("/assets/by-type")
    public ResponseEntity<List<AssetDistributionResponseDto>> getAssetDistributionData() {
        return ResponseEntity.ok(adminDashboardService.getAssetDistributionData());
    }

    @GetMapping("/assets/by-age")
    public ResponseEntity<List<AssetByAgeResponseDto>> getAverageAssetByAgeData() {
        return ResponseEntity.ok(adminDashboardService.getAverageAssetByAgeData());
    }

    @GetMapping("/assets/by-type-detailed")
    public ResponseEntity<List<DetailedAssetResponseDto>> getDetailedAssetData() {
        return ResponseEntity.ok(adminDashboardService.getDetailedAssetData());
    }
}
