package com.know_who_how.main_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // spring security exception
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "403", "접근권한이 없습니다."),
    NOT_LOGIN_USER(HttpStatus.UNAUTHORIZED, "401", "로그인하지 않은 사용자입니다."),

    // jwt token exception
    EMPTY_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "400", "토큰이 비어있습니다."),
    MALFORMED_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "401", "잘못된 JWT 형식입니다."),
    UNSUPPORTED_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "401", "지원하지 않는 JWT입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "401", "만료된 토큰입니다."),
    TOKEN_PARSING_FAILED(HttpStatus.UNAUTHORIZED, "401", "유효하지 않은 토큰 (파싱 실패)"),
    INVALID_TOKEN_SIGNATURE(HttpStatus.FORBIDDEN, "403", "JWT 서명 검증에 실패했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;    // HTTP 상태
    private final String code;          // API 응답에 사용할 커스텀 에러 코드 (HTTP 상태 코드와 동일하게)
    private final String message;       // API 응답에 사용할 에러 메시지
}