package com.know_who_how.main_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_001", "서버 내부 오류가 발생했습니다."),

    // spring security exception
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "SECURITY_001", "접근권한이 없습니다."),
    NOT_LOGIN_USER(HttpStatus.FORBIDDEN, "SECURITY_002", "로그인하지 않은 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "SECURITY_003", "비밀번호가 일치하지 않습니다."),

    // jwt token exception
    EMPTY_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "JWT_001", "토큰이 비어있습니다."),
    MALFORMED_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "JWT_002", "잘못된 JWT 형식입니다."),
    UNSUPPORTED_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "JWT_003", "지원하지 않는 JWT입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_004", "만료된 토큰입니다."),
    TOKEN_PARSING_FAILED(HttpStatus.UNAUTHORIZED, "JWT_005", "유효하지 않은 토큰 (파싱 실패)"),
    INVALID_TOKEN_SIGNATURE(HttpStatus.FORBIDDEN, "JWT_006", "JWT 서명 검증에 실패했습니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "JWT_007", "유효하지 않은 토큰 형식입니다."),

    // signup exception
    TERM_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_001", "존재하지 않는 약관 ID입니다."),
    INVALID_KEYWORD_VALUE(HttpStatus.BAD_REQUEST, "AUTH_002", "유효하지 않은 은퇴 키워드입니다."),
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH_003", "필수 약관에 동의해야 합니다."),
    LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "AUTH_004", "이미 사용 중인 아이디입니다."),
    PHONE_NUM_DUPLICATE(HttpStatus.CONFLICT, "AUTH_005", "이미 등록된" +
            " 전화번호입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_006", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_CERTIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_007", "유효하지 않은 인증 코드입니다."),
    CERTIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_008", "인증 코드가 만료되었습니다."),
    CERTIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_009", "인증 코드를 찾을 수 없습니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "AUTH_010", "유효하지 않은 전화번호 형식입니다."),
    ALREADY_LOGGED_OUT(HttpStatus.BAD_REQUEST, "AUTH_011", "이미 로그아웃된 토큰입니다.");


    private final HttpStatus status;    // HTTP 상태
    private final String code;          // API 응답에 사용할 커스텀 에러 코드 (HTTP 상태 코드와 동일하게)
    private final String message;       // API 응답에 사용할 에러 메시지
}