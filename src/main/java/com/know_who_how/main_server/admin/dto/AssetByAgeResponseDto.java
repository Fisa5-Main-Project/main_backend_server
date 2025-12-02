package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetByAgeResponseDto {
    private String ageGroup; // 연령대 (예: "20대", "30대")
    private BigDecimal averageAsset; // 해당 연령대의 평균 자산 금액
}
