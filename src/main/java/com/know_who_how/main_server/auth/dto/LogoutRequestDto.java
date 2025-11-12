package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequestDto {
    @Schema(description = "로그아웃할 사용자의 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MjcyNDU0NjB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    @NotBlank(message = "리프레시 토큰은 필수 입력 항목입니다.")
    private String refreshToken;
}
