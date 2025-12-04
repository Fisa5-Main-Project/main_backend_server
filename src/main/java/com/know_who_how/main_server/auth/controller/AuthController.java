package com.know_who_how.main_server.auth.controller;

import com.know_who_how.main_server.auth.dto.*;
import com.know_who_how.main_server.auth.service.AuthService;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.dto.ErrorResponse;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.global.jwt.JwtProperties;
import com.know_who_how.main_server.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "1. 인증/회원가입 API", description = "사용자 회원가입, 로그인, 로그아웃, 토큰 재발급 등 인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsCertificationService smsCertificationService;
    private final CookieUtil cookieUtil; // Added this

    @Operation(summary = "1-1. SMS 본인인증 문자 발송", description = """
            **사용 시점:** 회원가입 과정의 첫 단계, 본인인증 시 사용합니다.
            
            **성공:** `verificationId`가 담긴 응답을 반환합니다. 이 ID는 다음 단계(인증번호 확인)에서 필요합니다.
            
            **실패:**
            - 이미 가입된 전화번호인 경우 `409 Conflict` (AUTH_005)
            - 요청 DTO의 필드 유효성 검사 실패 시 `400 Bad Request`
            - SMS 발송 자체에 실패한 경우 `500 Internal Server Error` (AUTH_012)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 문자 발송 성공", content = @Content(schema = @Schema(implementation = SendSmsResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 전화번호 (PHONE_NUM_DUPLICATE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 (SMS 전송 실패 등)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup/send-sms")
    public ApiResponse<SendSmsResponseDto> sendSmsCertification(
            @Valid @RequestBody SmsCertificationRequestDto requestDto) {
        String verificationId = smsCertificationService.sendSmsCertification(requestDto);
        return ApiResponse.onSuccess(new SendSmsResponseDto(verificationId));
    }

    @Operation(summary = "1-2. SMS 인증번호 확인", description = """
            **사용 시점:** `1-1` 단계에서 문자를 받은 후, 사용자가 입력한 인증번호를 검증할 때 사용합니다.
            
            **성공:** "인증이 완료되었습니다." 메시지를 반환합니다.
            
            **실패:**
            - 인증번호가 틀린 경우 `400 Bad Request` (AUTH_007)
            - 인증 시간이 만료된 경우 `400 Bad Request` (AUTH_008)
            - 인증 요청 ID가 존재하지 않는 경우 `400 Bad Request` (AUTH_009)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = """
                    - 유효하지 않은 인증 코드 (AUTH_007)
                    - 인증 코드 만료 (AUTH_008)
                    - 인증 코드를 찾을 수 없음 (AUTH_009)
                    """, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup/check-code")
    public ApiResponse<String> confirmSmsCertification(@Valid @RequestBody SmsCertificationConfirmDto confirmDto) {
        String message = smsCertificationService.confirmSmsCertification(confirmDto);
        return ApiResponse.onSuccess(message);
    }

    @Operation(summary = "1-3. 아이디 중복 확인", description = """
            **사용 시점:** 회원가입 과정에서 사용자가 아이디를 입력할 때, 실시간으로 중복 여부를 확인하기 위해 호출합니다.
            
            **성공:** "사용 가능한 아이디입니다." 메시지를 반환합니다.
            
            **실패:**
            - 이미 사용 중인 아이디인 경우 `409 Conflict` (AUTH_004)
            - 아이디 형식(4~20자)에 맞지 않는 경우 `400 Bad Request`
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 아이디"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (아이디 형식 위반)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 (LOGIN_ID_DUPLICATE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/signup/check-login-id")
    public ApiResponse<String> checkLoginIdDuplicate(
            @Parameter(description = "중복 확인할 로그인 아이디 (4~20자)", required = true, example = "newuser123") @RequestParam @NotBlank(message = "아이디는 필수 입력 항목입니다.") @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.") String loginId) {
        String message = authService.checkLoginIdDuplicate(loginId);
        return ApiResponse.onSuccess(message);
    }

    @Operation(summary = "1-4. 전화번호 중복 확인", description = """
            **사용 시점:** 회원가입 과정에서 전화번호 중복 여부를 미리 확인하고 싶을 때 사용합니다. (주로 본인인증 단계에서 서버가 자동으로 체크하므로, 클라이언트에서 필수로 호출할 필요는 없을 수 있습니다.)
            
            **성공:** "사용 가능한 전화번호입니다." 메시지를 반환합니다.
            
            **실패:**
            - 이미 가입된 전화번호인 경우 `409 Conflict` (AUTH_005)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 전화번호"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 누락)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 전화번호 (PHONE_NUM_DUPLICATE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/signup/check-phone-num")
    public ApiResponse<String> checkPhoneNumDuplicate(
            @Parameter(description = "중복 확인할 전화번호 ('-' 제외)", required = true, example = "01012345678") @RequestParam @NotBlank(message = "전화번호는 필수 입력 항목입니다.") String phoneNum) {
        String message = authService.checkPhoneNumDuplicate(phoneNum);
        return ApiResponse.onSuccess(message);
    }

    @Operation(summary = "1-5. 최종 회원가입", description = """
            **두 가지 케이스에 사용되는 최종 회원가입 API입니다.**
            
            **1. 일반 회원가입:**
            - `signupToken` 필드를 제외하고 요청을 보냅니다.
            
            **2. 소셜 로그인 후 추가 정보 입력:**
            - 카카오 로그인 후, `isNewUser: true` 응답과 함께 받은 `signupToken` 값을 반드시 포함하여 요청해야 합니다.
            - 이 토큰을 통해 카카오 계정과 새로 생성되는 서비스 계정이 연동됩니다.
            
            **실패:**
            - `400 Bad Request`:
                - 필수 약관 미동의 (AUTH_003)
                - 유효하지 않은 `signupToken` (AUTH_016)
                - 기타 DTO 유효성 검사 실패
            - `409 Conflict`:
                - 아이디 중복 (AUTH_004)
                - 전화번호 중복 (AUTH_005)
                - 이미 연동된 소셜 계정 (AUTH_017)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 약관 미동의, 유효하지 않은 값 등)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 오류 (아이디, 전화번호, 소셜 계정)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup/submit")
    public ApiResponse<String> signup(@Valid @RequestBody UserSignupRequestDto requestDto) {
        authService.signup(requestDto);
        return ApiResponse.onSuccess("회원가입이 완료되었습니다.");
    }

    @Operation(summary = "1-6. 로그인", description = """
            **사용 시점:** 사용자가 아이디와 비밀번호로 로그인을 시도할 때 사용합니다.
            
            **성공:**
            - **응답 본문(Body):** Access Token 정보를 반환합니다.
            - **응답 헤더(Cookie):** Refresh Token이 `HttpOnly` 쿠키로 설정됩니다.
            
            **실패:**
            - 아이디가 존재하지 않는 경우 `404 Not Found` (AUTH_015)
            - 비밀번호가 틀린 경우 `401 Unauthorized` (SECURITY_003)
            - DTO 유효성 검사 실패 시 `400 Bad Request`
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = AccessTokenResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 누락)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (비밀번호 불일치)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 사용자", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ApiResponse<AccessTokenResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto,
                                                     HttpServletResponse response) {
        TokenResponseDto tokenDto = authService.login(requestDto);

        // Refresh Token을 HttpOnly 쿠키에 설정
        cookieUtil.setRefreshTokenCookie(response, tokenDto.getRefreshToken());

        // Access Token 정보만 DTO에 담아 Body로 반환
        AccessTokenResponseDto accessTokenResponse = AccessTokenResponseDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .build();

        return ApiResponse.onSuccess(accessTokenResponse);
    }

    @Operation(summary = "1-7. 로그아웃", description = """
            **사용 시점:** 사용자가 로그아웃을 요청할 때 사용합니다. **반드시 `Authorization` 헤더에 유효한 Access Token을 포함해야 합니다.**
            
            **성공:** "로그아웃이 완료되었습니다." 메시지를 반환하고, `refresh_token` 쿠키를 삭제합니다.
            
            **실패:**
            - `400 Bad Request`:
                - 이미 로그아웃 처리된 Access Token으로 다시 요청하는 경우 (AUTH_011)
            - `401 Unauthorized`:
                - `Authorization` 헤더가 없거나 토큰이 유효하지 않은 경우 (JWT_001, JWT_002, JWT_003, JWT_004, JWT_005, JWT_006, JWT_007)
                - `refresh_token` 쿠키가 유효하지 않은 경우 (AUTH_013)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 로그아웃된 토큰)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ApiResponse<String> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) { // required =
        // false로 설정하여 쿠키가
        // 없을 경우 null 처리
        String accessToken = resolveToken(request);
        if (accessToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_FORMAT);
        }
        authService.logout(accessToken, refreshToken);
        cookieUtil.deleteRefreshTokenCookie(response); // Refresh Token 쿠키 삭제
        return ApiResponse.onSuccess("로그아웃이 완료되었습니다.");
    }

    // Request Header에서 토큰 정보 추출 ( "Bearer [token]" )
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtAuthFilter.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Operation(summary = "1-8. Access Token 재발급", description = """
            **사용 시점:** API 요청 시 Access Token 만료(401 Unauthorized, `TOKEN_EXPIRED` 코드) 응답을 받았을 때, 새로운 Access Token을 발급받기 위해 호출합니다.
            
            **요청 방법:** 브라우저에 저장된 `refresh_token` 쿠키를 통해 자동으로 인증합니다. 클라이언트가 직접 헤더나 바디에 토큰을 담을 필요가 없습니다.
            
            **성공:** 새로운 Access Token을 반환합니다.
            
            **실패:**
            - `401 Unauthorized`:
                - Refresh Token이 만료되었거나 유효하지 않은 경우 (AUTH_013, JWT_004 등)
                - `refresh_token` 쿠키가 없는 경우
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Access Token 재발급 성공", content = @Content(schema = @Schema(implementation = AccessTokenResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 Refresh Token)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reissue")
    public ApiResponse<AccessTokenResponseDto> reissue(
            @CookieValue(name = "refresh_token") String refreshToken) {
        TokenResponseDto tokenResponseDto = authService.reissue(refreshToken);

        // Access Token 정보만 DTO에 담아 Body로 반환
        AccessTokenResponseDto accessTokenResponse = AccessTokenResponseDto.builder()
                .grantType(tokenResponseDto.getGrantType())
                .accessToken(tokenResponseDto.getAccessToken())
                .build();

        return ApiResponse.onSuccess(accessTokenResponse);
    }

    @Operation(summary = "1-1-1. 개발용) 테스트 SMS 인증 번호 전송", description = """
            **[개발 환경 전용]** 실제 SMS를 발송하지 않고, 본인인증 과정을 테스트하기 위한 API입니다.
            
            **사용 시점:** 회원가입 본인인증 과정 테스트 시, 실제 SMS 발송 비용 없이 인증 번호를 확인해야 할 때 사용합니다.
            
            **동작 방식:**
            - 실제 SMS를 발송하지 않고, 임의의 인증번호를 생성합니다.
            - 응답 Body에 `verificationId`와 함께 **생성된 `verificationCode`(인증번호)**를 직접 포함하여 반환합니다.
            
            **성공:** `verificationId`와 `authCode`가 담긴 응답을 반환합니다.
            
            **실패:**
            - 요청 DTO의 필드 유효성 검사 실패 시 `400 Bad Request`
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 인증 문자 발송 성공", content = @Content(schema = @Schema(implementation = TestSmsResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup/test-sms")
    public ApiResponse<TestSmsResponseDto> sendTestSmsCertification(
            @Valid @RequestBody SmsCertificationRequestDto requestDto) {
        TestSmsResponseDto testData = smsCertificationService.sendTestSmsCertification(requestDto);
        return ApiResponse.onSuccess(testData);
    }

    @Operation(summary = "2-1. 마이페이지 프로필 수정 본인인증 문자 발송",
            description = """
                    **사용 시점:** 프로필 수정 전 본인 확인을 위해 사용합니다.
                    
                    **성공:** `verificationId`가 담긴 응답을 반환합니다.
                    
                    **실패:**
                    - 요청 DTO의 필드 유효성 검사 실패 시 `400 Bad Request`
                    - SMS 발송 자체에 실패한 경우 `500 Internal Server Error` (AUTH_012)
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 문자 발송 성공", content = @Content(schema = @Schema(implementation = SendSmsResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류 (SMS 전송 실패 등)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/mypage/send-code")
    public ApiResponse<SendSmsResponseDto> sendSmsCertificationForMypage(@Valid @RequestBody MypageSmsSendRequestDto requestDto) {
        SendSmsResponseDto responseDto = smsCertificationService.sendSmsCertificationForMypage(requestDto);
        return ApiResponse.onSuccess(responseDto);
    }

    @Operation(summary = "2-2. 마이페이지 프로필 수정 SMS 인증번호 확인",
            description = """
                    **사용 시점:** 프로필 수정 본인 확인 문자 발송 후, 사용자가 입력한 인증번호를 검증할 때 사용합니다.
                    
                    **성공:** "인증이 완료되었습니다." 메시지를 반환합니다.
                    
                    **실패:**
                    - 인증번호가 틀린 경우 `400 Bad Request` (AUTH_007)
                    - 인증 시간이 만료된 경우 `400 Bad Request` (AUTH_008)
                    - 인증 요청 ID가 존재하지 않는 경우 `400 Bad Request` (AUTH_009)
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = """
                    - 유효하지 않은 인증 코드 (AUTH_007)
                    - 인증 코드 만료 (AUTH_008)
                    - 인증 코드를 찾을 수 없음 (AUTH_009)
                    """, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/mypage/check-code")
    public ApiResponse<String> confirmSmsCertificationForMypage(@Valid @RequestBody SmsCertificationConfirmDto confirmDto) {
        String message = smsCertificationService.confirmSmsCertificationForMypage(confirmDto);
        return ApiResponse.onSuccess(message);
    }
}