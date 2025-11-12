package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TestSmsResponseDto {
    @Schema(description = "SMS 인증 요청 ID. 다음 단계(인증번호 확인)에서 이 ID를 사용해야 합니다.", example = "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f")
    private String verificationId;
    @Schema(description = "테스트용으로 발급된 인증 코드. 실제 SMS는 발송되지 않습니다.", example = "123456")
    private String authCode;
}
