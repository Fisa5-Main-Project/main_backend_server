package com.know_who_how.main_server.asset_management.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "금융 상품 상세 정보 응답 DTO")
public class FinancialProductResponse {

    @Schema(description = "상품명", example = "WON플러스 예금")
    private final String productName;

    @Schema(description = "상품 타입 (DEPOSIT: 예금, SAVINGS: 적금)", example = "DEPOSIT")
    private final FinancialProduct.ProductType productType;

    @Schema(description = "기본 금리 (단일 금리 상품용)", example = "2.15")
    private final BigDecimal baseInterestRate;

    @Schema(description = "JSON 형태의 기간별 차등 금리 정보")
    private final String interestRateDetails;

    public FinancialProductResponse(FinancialProduct product) {
        this.productName = product.getProductName();
        this.productType = product.getProductType();
        this.baseInterestRate = product.getBaseInterestRate();
        this.interestRateDetails = product.getInterestRateDetails();
    }
}
