package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SmsCertificationConfirmDto {
    @Schema(description = "SMS 인증 요청 시 받은 ID", example = "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f")
    @NotBlank(message = "인증 ID는 필수 입력 항목입니다.")
    private String verificationId;

    @Schema(description = "SMS로 수신한 6자리 인증 코드", example = "123456")
    @NotBlank(message = "인증 코드는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String authCode;
}
