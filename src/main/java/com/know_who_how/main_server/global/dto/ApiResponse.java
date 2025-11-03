package com.know_who_how.main_server.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final Boolean isSuccess;
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ErrorResponse error;

    /**
     * 성공 또는 실패 응답을 생성하기 위한 private 생성자입니다.
     * 정적 팩토리 메서드(onSuccess, onFailure)를 통해서만 객체 생성이 가능하도록 합니다.
     *
     * @param isSuccess 성공 여부
     * @param data      성공 시 반환할 데이터
     * @param error     실패 시 반환할 에러 정보
     */
    private ApiResponse(Boolean isSuccess, T data, ErrorResponse error) {
        this.isSuccess = isSuccess;
        this.data = data;
        this.error = error;
    }
    public static <T> ApiResponse<T> onSuccess(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> onSuccess() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * API 요청이 실패했을 때 사용할 정적 팩토리 메서드입니다.
     *
     * @param code    에러 코드 (예: "USER_NOT_FOUND")
     * @param message 에러 메시지
     * @param <T>     데이터의 타입 (이 경우엔 null이므로 보통 Void)
     * @return ErrorResponse를 포함하는 실패 ApiResponse 객체
     */
    public static <T> ApiResponse<T> onFailure(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}
