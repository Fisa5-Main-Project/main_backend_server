package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SmsCertificationConfirmDto {
    @NotBlank(message = "인증 ID는 필수 입력 항목입니다.")
    private String verificationId;

    @NotBlank(message = "인증 코드는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String authCode;
}
