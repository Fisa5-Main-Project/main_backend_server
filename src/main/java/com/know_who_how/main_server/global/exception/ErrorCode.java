package com.know_who_how.main_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "입력 값이 올바르지 않습니다."),
    // ...

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "해당 사용자를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "해당 상품을 찾을 수 없습니다."),
    // ...

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류가 발생했습니다.");
    // ...

    private final HttpStatus status;
    private final String code; // ApiResponse.onFailure()에 사용할 커스텀 에러 코드
    private final String message; // ApiResponse.onFailure()에 사용할 에러 메시지
}