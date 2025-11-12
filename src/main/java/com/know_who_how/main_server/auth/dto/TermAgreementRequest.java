package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TermAgreementRequest {
    @Schema(description = "약관 ID", example = "1")
    @NotNull(message = "약관 ID는 필수 입력 항목입니다.")
    private Long termId;

    @Schema(description = "약관 동의 여부", example = "true")
    @NotNull(message = "약관 동의 여부는 필수 입력 항목입니다.")
    private Boolean isAgreed;
}
