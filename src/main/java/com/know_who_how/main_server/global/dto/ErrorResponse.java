package com.know_who_how.main_server.global.dto;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String code;
    private final String message;

    ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
