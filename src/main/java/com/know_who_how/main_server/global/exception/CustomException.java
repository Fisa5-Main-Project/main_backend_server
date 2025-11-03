package com.know_who_how.main_server.global.exception;

import lombok.Getter;

/**
 * 서비스 로직에서 에러가 발생했을 때 던지기 위한 커스텀 RuntimeException 입니다.
 * ErrorCode Enum을 멤버로 가집니다.
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        // 부모 생성자(RuntimeException)에 에러 메시지를 전달합니다.
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
