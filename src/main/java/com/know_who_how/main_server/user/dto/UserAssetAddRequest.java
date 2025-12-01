package com.know_who_how.main_server.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 추가 자산 정보 저장 요청 DTO
 * 부동산 자산 가액과 자동차 자산 가액을 포함합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "사용자 추가 자산 정보 저장 요청")
public class UserAssetAddRequest {

    @Schema(description = "부동산 자산 가액", example = "300000000")
    @PositiveOrZero(message = "부동산 자산 가액은 0 또는 양수여야 합니다.")
    private Long realEstate;

    @Schema(description = "자동차 자산 가액", example = "25000000")
    @PositiveOrZero(message = "자동차 자산 가액은 0 또는 양수여야 합니다.")
    private Long car;
}
