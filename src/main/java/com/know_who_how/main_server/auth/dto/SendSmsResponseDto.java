package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SendSmsResponseDto {
    @Schema(description = "SMS 인증 요청 ID. 다음 단계(인증번호 확인)에서 이 ID를 사용해야 합니다.", example = "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f")
    private String verificationId;
}
