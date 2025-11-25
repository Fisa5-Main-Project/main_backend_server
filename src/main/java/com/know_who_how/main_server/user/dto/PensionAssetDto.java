package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import com.know_who_how.main_server.global.entity.Asset.Pension.PensionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PensionAssetDto {

    @Schema(description = "자산 ID", example = "101")
    private Long assetId;

    @Schema(description = "자산 타입", example = "PENSION")
    private AssetType type;

    @Schema(description = "잔액", example = "15000000")
    private BigDecimal balance;

    @Schema(description = "은행 코드", example = "263")
    private String bankCode;

    private PensionDetails pensionDetails;

    @Getter
    @AllArgsConstructor
    public static class PensionDetails {
        private PensionType pensionType;
        private String accountName;
        private BigDecimal companyContrib;
        private BigDecimal personalContrib;
        private Integer contribYear;
        private BigDecimal totalPersonalContrib;
    }

    public static PensionAssetDto from(Asset asset, Pension pension) {
        PensionDetails details = null;
        if (pension != null) {
            details = new PensionDetails(
                    pension.getPensionType(),
                    pension.getAccountName(),
                    pension.getCompanyContrib(),
                    pension.getPersonalContrib(),
                    pension.getContribYear(),
                    pension.getTotalPersonalContrib()
            );
        }
        return new PensionAssetDto(
                asset.getAssetId(),
                asset.getType(),
                asset.getBalance(),
                asset.getBankCode(),
                details
        );
    }
}
