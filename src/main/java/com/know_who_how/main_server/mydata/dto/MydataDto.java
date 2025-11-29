package com.know_who_how.main_server.mydata.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "MyData 자산/부채 응답", example = """
        {
          "assets": [
            {
              "assetType": "SAVINGS",
              "bankCode": "088",
              "balance": 5000000,
              "updatedAt": "2025-11-24T10:39:40.1933239",
              "pensionDetails": null
            },
            {
              "assetType": "PENSION",
              "bankCode": "263",
              "balance": 15000000,
              "updatedAt": "2025-11-24T10:39:40.1933239",
              "pensionDetails": {
                "pensionType": "IRP",
                "accountName": "우리은행 개인형 IRP",
                "companyContrib": 0,
                "personalContrib": 5000000,
                "contribYear": 2025,
                "totalPersonalContrib": 12000000
              }
            },
            {
              "assetType": "PENSION",
              "bankCode": "263",
              "balance": 15000000,
              "updatedAt": "2025-11-24T10:39:40.1933239",
              "pensionDetails": {
                "pensionType": "DC",
                "accountName": "우리은행 퇴직연금 DC",
                "companyContrib": 3000000,
                "personalContrib": 1000000,
                "contribYear": 2025,
                "totalPersonalContrib": 0
              }
            }
          ],
          "liabilities": [
            {
              "liabilityId": 501,
              "bankCode": "020",
              "liabilityType": "LOAN",
              "balance": 50000000
            }
          ]
        }
        """)
public class MydataDto {
    @Schema(description = "자산 목록", example = """
            [
              {
                "assetType": "SAVINGS",
                "bankCode": "088",
                "balance": 5000000,
                "updatedAt": "2025-11-24T10:39:40.1933239",
                "pensionDetails": null
              },
              {
                "assetType": "PENSION",
                "bankCode": "263",
                "balance": 15000000,
                "updatedAt": "2025-11-24T10:39:40.1933239",
                "pensionDetails": {
                  "pensionType": "IRP",
                  "accountName": "우리은행 개인형 IRP",
                  "companyContrib": 0,
                  "personalContrib": 5000000,
                  "contribYear": 2025,
                  "totalPersonalContrib": 12000000
                }
              },
              {
                "assetType": "PENSION",
                "bankCode": "263",
                "balance": 15000000,
                "updatedAt": "2025-11-24T10:39:40.1933239",
                "pensionDetails": {
                  "pensionType": "DC",
                  "accountName": "우리은행 퇴직연금 DC",
                  "companyContrib": 3000000,
                  "personalContrib": 1000000,
                  "contribYear": 2025,
                  "totalPersonalContrib": 0
                }
              }
            ]
            """)
    private List<AssetDto> assets;

    @Schema(description = "부채 목록", example = "[{\"liabilityId\":501,\"bankCode\":\"020\",\"liabilityType\":\"LOAN\",\"balance\":50000000}]")
    private List<LiabilityDto> liabilities;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetDto {
        @Schema(description = "자산 유형", example = "PENSION")
        private String assetType;
        @Schema(description = "은행 코드", example = "263")
        private String bankCode;
        @Schema(description = "잔액", example = "15000000")
        private BigDecimal balance;
        @Schema(description = "업데이트 시각", example = "2025-11-24T10:39:40.1933239")
        private String updatedAt;
        @Schema(description = "연금 세부 정보 (연금 자산일 때)", implementation = PensionDetailsDto.class)
        private PensionDetailsDto pensionDetails;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PensionDetailsDto {
        @Schema(description = "연금 유형", example = "IRP")
        private String pensionType;
        @Schema(description = "계좌 이름", example = "우리은행 개인형 IRP")
        private String accountName;
        @Schema(description = "회사 납입금", example = "0")
        private BigDecimal companyContrib;
        @Schema(description = "개인 납입금", example = "5000000")
        private BigDecimal personalContrib;
        @Schema(description = "납입 연도", example = "2025")
        private Integer contribYear;
        @Schema(description = "총 개인 납입액", example = "12000000")
        private BigDecimal totalPersonalContrib;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LiabilityDto {
        @Schema(description = "부채 ID", example = "501")
        private Long liabilityId;
        @Schema(description = "은행 코드", example = "020")
        private String bankCode;
        @Schema(description = "부채 유형", example = "LOAN")
        private String liabilityType;
        @Schema(description = "잔액", example = "50000000")
        private BigDecimal balance;
    }
}
