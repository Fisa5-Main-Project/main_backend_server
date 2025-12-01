package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InvestmentTendencyUpdateRequestDto {
    @Schema(description = "수정할 투자 성향", example = "안정추구형")
    @NotNull(message = "투자 성향은 필수 입력 항목입니다.")
    private InvestmentTendancy investmentTendancy;
}