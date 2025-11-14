package com.know_who_how.main_server.asset_management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendedProductDto(
        String productName,
        String productType, // 예: "예적금", "연금저축", "펀드"
        String rate,        // 예: "연 3.5%", "수익률 12.3%"
        String bankName,    // 예: "우리은행"
        String description  // 예: "세액공제 16.5%" (선택 사항)
) {
}
