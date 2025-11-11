package com.know_who_how.main_server.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponseDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
}
