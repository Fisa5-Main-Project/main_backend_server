package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDto {
    @Schema(description = "토큰 타입", example = "Bearer")
    private String grantType;
    @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlcjEiLCJhdXRoIjoiUk9MRV9VU0VSIiwiZXhwIjoxNjI2NjQwNjYwfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String accessToken;
    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MjcyNDU0NjB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String refreshToken;
}
