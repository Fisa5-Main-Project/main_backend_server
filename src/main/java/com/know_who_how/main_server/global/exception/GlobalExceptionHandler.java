package com.know_who_how.main_server.global.exception;

import com.know_who_how.main_server.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 어노테이션을 사용한 DTO의 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     *
     * @param e MethodArgumentNotValidException
     * @return 400 Bad Request와 첫 번째 유효성 검증 실패 메시지
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();

        // 유효성 검증 실패 메시지를 동적으로 생성
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage()
                : ErrorCode.INVALID_INPUT_VALUE.getMessage();
        log.warn("MethodArgumentNotValidException: {}", errorMessage);

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return new ResponseEntity<>(
                ApiResponse.onFailure(errorCode.getCode(), errorMessage),
                errorCode.getStatus());
    }

    /**
     * Spring Security의 BadCredentialsException (비밀번호 불일치)을 처리합니다.
     *
     * @param e BadCredentialsException
     * @return INVALID_PASSWORD 에러 응답
     */
    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("BadCredentialsException: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_PASSWORD;
        return new ResponseEntity<>(
                ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()),
                errorCode.getStatus());
    }

    /**
     * 필수 파라미터가 누락되었을 때 발생하는 예외를 처리합니다.
     *
     * @param e MissingServletRequestParameterException
     * @return INVALID_PARAMETER 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMissingParams(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;

        // 누락된 파라미터 명을 에러 메세지에 포함합니다.
        String message = String.format("%s (%s)", errorCode.getMessage(), e.getParameterName());

        return new ResponseEntity<>(
                ApiResponse.onFailure(errorCode.getCode(), message),
                errorCode.getStatus());
    }

    /**
     * 서비스 로직에서 발생하는 모든 CustomException을 처리합니다.
     *
     * @param e CustomException
     * @return ApiResponse.onFailure()를 사용한 일관된 에러 응답
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        // ErrorCode에서 정의한 code와 message를 가져옵니다.
        ErrorCode errorCode = e.getErrorCode();
        String code = errorCode.getCode();
        String message = errorCode.getMessage();
        HttpStatus status = errorCode.getStatus();

        // 로그 기록
        // (실제 배포 시: e.getStackTrace()를 포함하여 더 자세한 로그 필요)
        log.error("Handling CustomException: {} - {}", code, message, e);

        // ApiResponse.onFailure()를 사용하여 실패 응답을 생성합니다.
        // HTTP Status도 ErrorCode에서 정의한 것을 사용합니다.
        return new ResponseEntity<>(
                ApiResponse.onFailure(code, message),
                status);
    }

    /**
     * 위에서 처리하지 못한 모든 예외를 처리합니다. (Catch-all)
     *
     * @param e Exception
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 예외 스택 트레이스를 로그로 남깁니다.
        log.error("Unhandled exception caught!", e);

        // ErrorCode에 정의된 INTERNAL_SERVER_ERROR를 사용합니다.
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        // ApiResponse.onFailure()를 사용하여 실패 응답을 생성합니다.
        return new ResponseEntity<>(
                ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()),
                errorCode.getStatus());
    }

    /**
     * Spring Security 인증 과정에서 발생하는 내부 에러를 처리합니다.
     * CustomUserDetailsService에서 발생시킨 CustomException이 이 예외로 감싸져서 넘어옵니다.
     *
     * @param e InternalAuthenticationServiceException
     * @return CustomException의 내용으로 에러 응답
     */
    @ExceptionHandler(org.springframework.security.authentication.InternalAuthenticationServiceException.class)
    protected ResponseEntity<ApiResponse<Void>> handleInternalAuthenticationServiceException(
            org.springframework.security.authentication.InternalAuthenticationServiceException e) {
        // 원인 예외가 CustomException인지 확인
        if (e.getCause() instanceof CustomException) {
            CustomException customException = (CustomException) e.getCause();
            return handleCustomException(customException);
        }

        // CustomException이 아닌 경우 일반적인 서버 에러로 처리
        log.error("InternalAuthenticationServiceException: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(
                ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()),
                errorCode.getStatus());
    }

}
