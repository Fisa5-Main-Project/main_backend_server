package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetDistributionResponseDto {
    private String name;
    private BigDecimal value; // 총 자산 규모 또는 해당 자산 타입의 비율
    private String color; // 차트 표시용 색상 코드
    private BigDecimal average; // 해당 타입의 평균 자산
}
