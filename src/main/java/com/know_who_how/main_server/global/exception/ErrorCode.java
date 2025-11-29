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
    PHONE_NUM_DUPLICATE(HttpStatus.CONFLICT, "AUTH_005", "이미 등록된 전화번호입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_006", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_CERTIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_007", "유효하지 않은 인증 코드입니다."),
    CERTIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_008", "인증 코드가 만료되었습니다."),
    CERTIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH_009", "인증 코드를 찾을 수 없습니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "AUTH_010", "유효하지 않은 전화번호 형식입니다."),
    ALREADY_LOGGED_OUT(HttpStatus.BAD_REQUEST, "AUTH_011", "이미 로그아웃된 토큰입니다."),
    SMS_SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_012", "SMS 전송에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_013", "유효하지 않은 리프레시 토큰입니다."),
    SECURITY_CONTEXT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_014", "인증 정보를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_015", "해당 사용자를 찾을 수 없습니다."),
    SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "AUTH_016", "유효하지 않은 회원가입 토큰입니다."),
    SOCIAL_ACCOUNT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "AUTH_017", "이미 해당 소셜 계정으로 가입된 유저입니다."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_018", "소셜 로그인에 실패했습니다."),

    // Common Exception
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 유효하지 않습니다."),

    // portfolio exception
    USER_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "PORT_001", "사용자의 재무 정보를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PORT_002", "해당 금융 상품을 찾을 수 없습니다."),

    // job exception
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "JOB_001", "해당 ID의 채용 공고를 찾을 수 없습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "JOB_002", "필수 파라미터가 누락되거나 유효하지 않습니다."),

    // External Open API Exception
    EXTERNAL_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "EXTERNAL_001", "Open API 인증키가 유효하지 않습니다."),
    EXTERNAL_API_FORBIDDEN(HttpStatus.FORBIDDEN, "EXTERNAL_002", "Open API 서비스 접근 권한이 없습니다.(신청/승인 상태 확인)"),
    EXTERNAL_API_NOT_FOUND(HttpStatus.NOT_FOUND, "EXTERNAL_003", "Open API 서비스가 존재하지 않습니다.(URL 확인 필요)"),
    EXTERNAL_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "EXTERNAL_004","Open API 일일 호출 허용량을 초과했습니다."),
    EXTERNAL_API_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_005", "기관 API 서버로부터 응답을 받지 못했습니다."),

    // Mydata Exception
    MYDATA_EXPIRED(HttpStatus.UNAUTHORIZED, "MYDATA_001", "마이데이터 연동 토큰이 만료되었습니다. 재연동이 필요합니다."),
    MYDATA_NOT_LINKED(HttpStatus.BAD_REQUEST, "MYDATA_002", "마이데이터 연동 정보(토큰)가 없습니다."),
    MYDATA_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MYDATA_003", "마이데이터 토근 교환 오류가 발생했습니다."),

    EXTERNAL_API_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_005", "기관 API 서버로부터 응답을 받지 못했습니다."),

    // inheritance exception
    INHERITANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "INHERITANCE_001", "요청하신 상속 정보를 찾을 수 없습니다."),
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "INHERITANCE_002", "해당 영상편지 정보를 찾을 수 없습니다."),
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INHERITANCE_003", "해당 수신자 정보를 찾을 수 없습니다."),
    FORBIDDEN_INHERITANCE_ACCESS(HttpStatus.FORBIDDEN, "INHERITANCE_004", "해당 상속 정보에 접근 권한이 없습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INHERITANCE_005", "유효하지 않거나 만료된 영상 접근 토큰입니다."),
    VIDEO_ALREADY_EXISTS(HttpStatus.CONFLICT, "INHERITANCE_006", "이미 등록된 영상편지가 존재합니다. 삭제 후 다시 시도해주세요.");

    private final HttpStatus status;    // HTTP 상태
    private final String code;          // API 응답에 사용할 커스텀 에러 코드 (HTTP 상태 코드와 동일하게)
    private final String message;       // API 응답에 사용할 에러 메시지
}