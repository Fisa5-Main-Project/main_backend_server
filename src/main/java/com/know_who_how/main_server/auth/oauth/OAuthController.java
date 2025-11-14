package com.know_who_how.main_server.auth.oauth;

import com.know_who_how.main_server.auth.oauth.dto.OAuthResult;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtProperties;
import com.know_who_how.main_server.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1-1. 소셜 로그인 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/login/oauth2/code")
public class OAuthController {

    private final OAuthService oAuthService;
    private final CookieUtil cookieUtil;

    @Operation(summary = "카카오 로그인 콜백 처리",
            description = """
                    **[중요] 클라이언트가 직접 호출하는 API가 아닙니다.**
                    
                    카카오 로그인 성공 시, 카카오 서버가 사용자의 브라우저를 이 주소로 리다이렉트시킵니다.
                    백엔드는 리다이렉트 요청에 포함된 `code`(인가 코드)를 사용하여 로그인/회원가입 로직을 처리합니다.
                    
                    - **기존 회원**: `isNewUser: false`와 함께 `accessToken`을 반환하고, `refreshToken`은 `HttpOnly` 쿠키에 담아 전달합니다.
                    - **신규 회원**: `isNewUser: true`와 함께 추가 정보 입력을 위한 `signupToken`을 반환합니다.
                    """)
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "소셜 로그인 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "기존 회원 로그인 성공",
                                            summary = "기존 회원 로그인 예시 (Access Token은 Body, Refresh Token은 HttpOnly 쿠키로 전달)",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "data": {
                                                        "isNewUser": false,
                                                        "grantType": "Bearer",
                                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
                                                      },
                                                      "error": null
                                                    }"""),
                                    @ExampleObject(name = "신규 회원 가입 필요",
                                            summary = "신규 회원 가입 예시",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "data": {
                                                        "isNewUser": true,
                                                        "signupToken": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                                                      },
                                                      "error": null
                                                    }""")
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "소셜 로그인 실패 (사용자 취소 등)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
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
    public ApiResponse<OAuthResult> oauth2Login(
            @PathVariable String registrationId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletResponse response) {

        if (error != null) {
            log.error("카카오 로그인 에러 발생 - error: {}, error_description: {}", error, errorDescription);
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        if (code == null) {
            log.error("카카오 로그인 콜백에 인가 코드가 없습니다.");
            throw new CustomException(ErrorCode.OAUTH_LOGIN_FAILED);
        }

        log.info("카카오 로그인 콜백 요청 - registrationId: {}, code: {}", registrationId, code);
        OAuthResult result = oAuthService.oauthLogin(registrationId, code);

        // 응답 분기 처리
        if (!result.getIsNewUser()) {
            // 기존 회원: Refresh Token은 쿠키로, Access Token 등은 바디로 전달
            cookieUtil.setRefreshTokenCookie(response, result.getRefreshToken());
        }

        return ApiResponse.onSuccess(result);
    }


}
