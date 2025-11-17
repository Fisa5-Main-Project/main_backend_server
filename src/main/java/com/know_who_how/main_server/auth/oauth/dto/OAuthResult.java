package com.know_who_how.main_server.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthResult {
    private Boolean isNewUser;
    private String signupToken;
    private String accessToken;
    private String refreshToken;
}
