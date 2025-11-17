package com.know_who_how.main_server.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.know_who_how.main_server.auth.dto.AccessTokenResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 필드는 JSON에서 제외
public class OAuthLoginResponse {
    // 신규 회원 여부
    private Boolean isNewUser;

    // 기존 회원일 경우, 로그인 성공 시 발급되는 토큰 정보
    private AccessTokenResponseDto tokenInfo;

    // 신규 회원일 경우, 회원가입 프로세스를 이어가기 위한 임시 토큰
    private String signupToken;
}
