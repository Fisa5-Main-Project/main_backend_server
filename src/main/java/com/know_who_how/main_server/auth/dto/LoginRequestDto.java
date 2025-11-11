package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;
}
