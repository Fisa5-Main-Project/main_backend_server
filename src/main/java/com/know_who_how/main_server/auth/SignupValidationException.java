package com.know_who_how.main_server.auth;

import lombok.Getter;

/**
 * 회원가입 관련 유효성 검증 실패 시 사용할 커스텀 예외
 */
@Getter
public class SignupValidationException extends RuntimeException {
    private final String code; // ApiResponse의 error.code에 담길 값

    public SignupValidationException(String code, String message) {
        super(message);
        this.code = code;
    }
}

