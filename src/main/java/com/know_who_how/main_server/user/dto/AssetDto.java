package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AssetDto {
    @Schema(description = "자산 ID", example = "101")
    private Long assetId;

    @Schema(description = "자산 유형", example = "PENSION")
    private AssetType type;

    @Schema(description = "잔액", example = "15000000")
    private BigDecimal balance;

    @Schema(description = "은행 코드", example = "263")
    private String bankCode;

    public static AssetDto from(Asset asset) {
        return new AssetDto(
                asset.getAssetId(),
                asset.getType(),
                asset.getBalance(),
                asset.getBankCode()
        );
    }
}
