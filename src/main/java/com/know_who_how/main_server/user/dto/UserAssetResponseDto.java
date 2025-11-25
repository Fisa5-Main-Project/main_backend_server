package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.Asset.Asset;
import com.know_who_how.main_server.global.entity.Asset.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAssetResponseDto {
    private Long userId;
    private Long assetId;
    private AssetType type;
    private BigDecimal balance;
    private String bankCode;

    public static UserAssetResponseDto from(Asset asset) {
        return UserAssetResponseDto.builder()
                .userId(asset.getUser().getUserId())
                .assetId(asset.getAssetId())
                .type(asset.getType())
                .balance(asset.getBalance())
                .bankCode(asset.getBankCode())
                .build();
    }
}
