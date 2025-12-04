package com.know_who_how.main_server.auth.oauth;

import com.know_who_how.main_server.auth.oauth.dto.OAuthResult;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@Tag(name = "1-1. 소셜 로그인 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/login/oauth2/code")
public class OAuthController {

    @Value("${app.client-base-url}")
    private String clientBaseUrl;

    private final OAuthService oAuthService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "카카오 로그인 콜백 처리", description = """
            **[중요] 클라이언트가 직접 호출하는 API가 아닙니다.**

            카카오 로그인 성공 시, 카카오 서버가 사용자의 브라우저를 이 주소로 리다이렉트시킵니다.
            백엔드는 리다이렉트 요청에 포함된 `code`(인가 코드)를 사용하여 로그인/회원가입 로직을 처리한 후,
            결과값(토큰 등)을 쿼리 파라미터에 담아 **클라이언트 페이지로 다시 리다이렉트**시킵니다.

            - **기존 회원**: `isNewUser=false`, `accessToken`을 쿼리 파라미터로 전달하고, `refreshToken`은 `HttpOnly` 쿠키에 담아 전달합니다.
            - **신규 회원**: `isNewUser=true`, `signupToken`을 쿼리 파라미터로 전달합니다.
            """)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "로그인/회원가입 처리 후 클라이언트 페이지로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "소셜 로그인 실패 (사용자 취소 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
                    {
                      "isSuccess": false,
                      "data": null,
                      "error": {
                        "code": "AUTH_018",
                        "message": "소셜 로그인에 실패했습니다."
                      }
                    }""")))
    })
    @GetMapping("/{registrationId}")
    public void oauth2Login(
            @PathVariable String registrationId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            log.error("카카오 로그인 에러 발생 - error: {}, error_description: {}", error, errorDescription);
            response.sendRedirect(clientBaseUrl + "/login?error=social_login_failed");
            return;
        }

        if (code == null) {
            log.error("카카오 로그인 콜백에 인가 코드가 없습니다.");
            // 이 경우, 에러 페이지로 리다이렉트하거나 예외를 발생시킬 수 있습니다.
            // 여기서는 예외를 발생시켜 GlobalExceptionHandler가 처리하도록 합니다.
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        log.info("카카오 로그인 콜백 요청 - registrationId: {}, code: {}", registrationId, code);
        OAuthResult result = oAuthService.oauthLogin(registrationId, code);

        // 클라이언트 리다이렉트 URL 생성
        String clientRedirectUrl = clientBaseUrl + "/oauth/callback";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(clientRedirectUrl)
                .queryParam("isNewUser", result.getIsNewUser());

        if (result.getIsNewUser()) {
            // 신규 회원
            uriBuilder.queryParam("signupToken", result.getSignupToken());
        } else {
            // 기존 회원: Refresh Token은 쿠키로, Access Token은 쿼리 파라미터로 전달
            cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
            uriBuilder.queryParam("accessToken", result.getAccessToken());
        }

        String targetUrl = uriBuilder.build().toUriString();
        log.info("클라이언트로 리다이렉트: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

}
