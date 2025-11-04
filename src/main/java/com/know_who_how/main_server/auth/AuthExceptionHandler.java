package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.global.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * auth 패키지 전용 예외 처리기
 */
@RestControllerAdvice(basePackages = "com.know_who_how.main_server.auth")
public class AuthExceptionHandler {

    /**
     * 1. @Valid 유효성 검사 실패 시 (@RequestBody DTO 검증)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String code = "400_VALID";
        // 여러 에러 중 첫 번째 에러 메시지를 사용
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.onFailure(code, message));
    }

    /**
     * 2. @Validated 유효성 검사 실패 시 (@RequestParam 검증)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(ConstraintViolationException ex) {
        String code = "400_VALID_PARAM";
        // 여러 에러 중 첫 번째 에러 메시지를 사용
        String message = ex.getConstraintViolations().iterator().next().getMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.onFailure(code, message));
    }

    /**
     * 3. 서비스 로직 검증 실패 시 (ID/전화번호 중복, 약관 미동의 등)
     */
    @ExceptionHandler(SignupValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleSignupValidationException(SignupValidationException ex) {
        // ID/전화번호 중복 시 409 Conflict, 그 외 400 Bad Request
        HttpStatus status = ex.getCode().startsWith("409") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(ApiResponse.onFailure(ex.getCode(), ex.getMessage()));
    }

    /**
     * 4. 기타 처리되지 않은 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception ex) {
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(ApiResponse.onFailure("500_SERVER", "서버 내부 오류가 발생했습니다."));
    }
}

