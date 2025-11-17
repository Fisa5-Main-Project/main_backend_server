package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    @Schema(description = "로그인 아이디", example = "testuser1")
    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password123!")
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;
}
