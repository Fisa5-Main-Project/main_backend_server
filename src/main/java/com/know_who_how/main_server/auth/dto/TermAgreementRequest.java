package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TermAgreementRequest {
    @NotNull(message = "약관 ID는 필수 입력 항목입니다.")
    private Long termId;

    @NotNull(message = "약관 동의 여부는 필수 입력 항목입니다.")
    private Boolean isAgreed;
}
