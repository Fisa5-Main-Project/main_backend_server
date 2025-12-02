package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedAssetResponseDto {
    private String type; // 자산 타입 (예: "예금", "투자")
    private BigDecimal totalAsset; // 해당 타입의 총 자산
    private BigDecimal averageBalance; // 해당 타입의 평균 잔액
    private double ratio; // 해당 타입이 전체 자산에서 차지하는 비율 (예: 37.5)
}
