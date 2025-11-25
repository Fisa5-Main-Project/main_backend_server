package com.know_who_how.main_server.global.exception;

import com.know_who_how.main_server.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
         * 인증 과정에서 발생하는 내부 로직 오류를 처리합니다.
         * (예: UserDetailsService에서 CustomException 발생 시 이를 감싸서 던짐)
         *
         * @param e InternalAuthenticationServiceException
         * @return 원인 예외에 따른 에러 응답
         */
        @ExceptionHandler(InternalAuthenticationServiceException.class)
        protected ResponseEntity<ApiResponse<Void>> handleInternalAuthenticationServiceException(
                        InternalAuthenticationServiceException e) {
                // 원인 예외가 CustomException인 경우, 해당 예외의 ErrorCode를 사용
                if (e.getCause() instanceof CustomException) {
                        CustomException customException = (CustomException) e.getCause();
                        return handleCustomException(customException);
                }

                log.error("InternalAuthenticationServiceException: {}", e.getMessage(), e);
                ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
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
         * WebClient 요청 중 발생하는 예외를 처리합니다.
         *
         * @param e WebClientResponseException
         * @return 외부 서비스로부터 받은 상태 코드와 메시지
         */
        @ExceptionHandler(WebClientResponseException.class)
        protected ResponseEntity<ApiResponse<Void>> handleWebClientResponseException(WebClientResponseException e) {
                log.error("WebClientResponseException: Status={}, Body={}", e.getStatusCode(),
                                e.getResponseBodyAsString(), e);

                return new ResponseEntity<>(
                                ApiResponse.onFailure(String.valueOf(e.getStatusCode().value()),
                                                e.getResponseBodyAsString()),
                                e.getStatusCode());
        }

        /**
         * WebClient 요청 중 연결 오류(타임아웃, 서버 다운 등)가 발생했을 때 처리합니다.
         *
         * @param e WebClientRequestException
         * @return EXTERNAL_API_SERVER_ERROR 에러 응답
         */
        @ExceptionHandler(WebClientRequestException.class)
        protected ResponseEntity<ApiResponse<Void>> handleWebClientRequestException(WebClientRequestException e) {
                log.error("WebClientRequestException: {}", e.getMessage(), e);

                ErrorCode errorCode = ErrorCode.EXTERNAL_API_SERVER_ERROR;
                return new ResponseEntity<>(
                                ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage()),
                                errorCode.getStatus());
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

}
