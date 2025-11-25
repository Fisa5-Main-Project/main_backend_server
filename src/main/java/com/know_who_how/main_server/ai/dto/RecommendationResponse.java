package com.know_who_how.main_server.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationResponse {
    private RecommendedProduct depositOrSaving;
    private RecommendedProduct annuity;
    private RecommendedProduct fund;

    @Getter
    @NoArgsConstructor
    public static class RecommendedProduct {
        private String productType;
        private String productName;
        private String companyName;
        private String benefit;
        private String reason;
    }
}
